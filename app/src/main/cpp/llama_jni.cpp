#include <jni.h>
#include <string>
#include "llama.h"

#include <android/log.h>
#include <string>
#include <mutex>
#include <vector>
#include <atomic>

#define LOG_TAG "LlamaJNI_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static void native_log_callback(enum ggml_log_level level, const char * text, void * user_data) {
    if (level == GGML_LOG_LEVEL_ERROR) {
        LOGE("llama: %s", text);
    } else if (level == GGML_LOG_LEVEL_WARN) {
        LOGW("llama: %s", text);
    } else {
        LOGI("llama: %s", text);
    }
}

struct llama_instance {
    llama_model* model;
    llama_context* ctx;
};

// Ensure backend is initialized only once
static bool g_backend_initialized = false;

extern "C" JNIEXPORT jstring JNICALL
Java_com_example_slmlocal_ai_LlamaJNI_systemInfo(
        JNIEnv* env,
        jobject /* this */) {
    if (!g_backend_initialized) {
        llama_log_set(native_log_callback, nullptr);
        llama_backend_init();
        g_backend_initialized = true;
    }
    const char* sys_info = llama_print_system_info();
    return env->NewStringUTF(sys_info);
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_example_slmlocal_ai_LlamaJNI_loadModel(
        JNIEnv* env,
        jobject /* this */,
        jstring modelPathStr,
        jint contextSize) {
    
    if (!g_backend_initialized) {
        llama_backend_init();
        g_backend_initialized = true;
    }

    const char* path = env->GetStringUTFChars(modelPathStr, nullptr);
    LOGI("Loading model from: %s", path);

    llama_model_params model_params = llama_model_default_params();
    
    int64_t t_start_ms = llama_time_us() / 1000;
    
    llama_model* model = llama_model_load_from_file(path, model_params);
    if (!model) {
        LOGE("Failed to load model from %s", path);
        env->ReleaseStringUTFChars(modelPathStr, path);
        return (jlong) 0;
    }
    
    LOGI("Model loaded successfully. Initializing context...");
    
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = contextSize; // Set safely to 4096 tokens
    ctx_params.n_threads = 4;       // Conservative thread count for mobile
    ctx_params.n_threads_batch = 4;
    
    llama_context* ctx = llama_init_from_model(model, ctx_params);
    if (!ctx) {
        LOGE("Failed to initialize context from model.");
        llama_model_free(model);
        env->ReleaseStringUTFChars(modelPathStr, path);
        return (jlong) 0;
    }
    
    int64_t t_end_ms = llama_time_us() / 1000;
    LOGI("Model and context loaded in %lld ms.", (long long)(t_end_ms - t_start_ms));

    env->ReleaseStringUTFChars(modelPathStr, path);
    
    llama_instance* inst = new llama_instance();
    inst->model = model;
    inst->ctx = ctx;

    return (jlong) inst;
}

#include <vector>
#include <atomic>

static std::atomic<bool> g_cancel_generation(false);

extern "C" JNIEXPORT void JNICALL
Java_com_example_slmlocal_ai_LlamaJNI_cancelGeneration(
        JNIEnv* env,
        jobject /* this */) {
    g_cancel_generation = true;
    LOGI("Generation cancellation requested.");
}

static void my_llama_batch_add(struct llama_batch & batch, llama_token id, llama_pos pos, const std::vector<llama_seq_id> & seq_ids, bool logits) {
    batch.token   [batch.n_tokens] = id;
    batch.pos     [batch.n_tokens] = pos;
    batch.n_seq_id[batch.n_tokens] = seq_ids.size();
    for (size_t i = 0; i < seq_ids.size(); ++i) {
        batch.seq_id[batch.n_tokens][i] = seq_ids[i];
    }
    batch.logits  [batch.n_tokens] = logits ? 1 : 0;
    batch.n_tokens++;
}

static void my_llama_batch_clear(struct llama_batch & batch) {
    batch.n_tokens = 0;
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_slmlocal_ai_LlamaJNI_generateTokens(
        JNIEnv* env,
        jobject /* this */,
        jlong contextPtr,
        jstring prompt,
        jobject callback) {
    
    g_cancel_generation = false;
    llama_instance* inst = (llama_instance*) contextPtr;
    if (!inst || !inst->model || !inst->ctx) {
        LOGE("Invalid model/context pointer");
        return;
    }

    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    
    // 1. Format prompt (Hardcoded for Gemma)
    std::string formatted_prompt = std::string("<start_of_turn>user\n") + prompt_cstr + "<end_of_turn>\n<start_of_turn>model\n";
    
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
    LOGI("Formatted prompt: %s", formatted_prompt.c_str());

    // 2. Tokenize
    LOGI("Tokenizing prompt...");
    const llama_vocab* vocab = llama_model_get_vocab(inst->model);
    int max_tokens = formatted_prompt.length() + 128;
    std::vector<llama_token> tokens(max_tokens);
    int32_t n_tokens = llama_tokenize(vocab, formatted_prompt.c_str(), formatted_prompt.length(), tokens.data(), max_tokens, true, true);
    
    if (n_tokens < 0) {
        LOGE("Tokenization failed with error code: %d", n_tokens);
        return;
    }
    tokens.resize(n_tokens);
    LOGI("Prompt tokenization result: %d tokens", n_tokens);

    // Get JNI Callback method
    jclass cbClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(cbClass, "onToken", "(Ljava/lang/String;)V");
    if (!onTokenMethod) {
        LOGE("Failed to find onToken method");
        return;
    }
    
    int64_t t_start_eval = llama_time_us();

    // Clear KV cache before generation
    LOGI("Clearing KV cache for new generation...");
    llama_memory_t mem = llama_get_memory(inst->ctx);
    llama_memory_seq_rm(mem, -1, -1, -1);


    // 3. Batch and decode prompt
    LOGI("Initializing llama_batch...");
    llama_batch batch = llama_batch_init(n_tokens, 0, 1);
    my_llama_batch_clear(batch);
    for (int i = 0; i < n_tokens; ++i) {
        my_llama_batch_add(batch, tokens[i], i, {0}, false);
    }
    batch.logits[batch.n_tokens - 1] = 1; // Only get logits for the last token of the prompt
    LOGI("llama_batch initialized with %d tokens", batch.n_tokens);
    
    LOGI("Diagnostics before llama_decode:");
    LOGI("context pointer: %p", inst->ctx);
    LOGI("model pointer: %p", inst->model);
    LOGI("n_ctx: %u", llama_n_ctx(inst->ctx));
    LOGI("n_batch: %u", llama_n_batch(inst->ctx));
    LOGI("n_ubatch: %u", llama_n_ubatch(inst->ctx));
    LOGI("batch.n_tokens: %d", batch.n_tokens);
    
    LOGI("Calling initial llama_decode()...");
    int decode_res = llama_decode(inst->ctx, batch);
    LOGI("Initial llama_decode() returned: %d", decode_res);
    if (decode_res != 0) {
        LOGE("llama_decode failed with error code: %d", decode_res);
        return;
    }
    
    int64_t t_first_token = llama_time_us();
    
    // 4. Sampler
    LOGI("Initializing sampler...");
    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    sparams.no_perf = false;
    llama_sampler* smpl = llama_sampler_chain_init(sparams);
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.8f));
    llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    LOGI("Sampler initialized.");
    
    int current_pos = n_tokens;
    int max_gen_tokens = 512;
    int gen_count = 0;
    
    LOGI("Starting generation loop...");
    while (gen_count < max_gen_tokens && !g_cancel_generation) {
        LOGI("Diagnostics before llama_sampler_sample:");
        LOGI("- sampled token position: -1");
        LOGI("- batch.n_tokens: %d", batch.n_tokens);
        LOGI("- last token ID: %d", batch.token[batch.n_tokens - 1]);
        LOGI("- logits flag for the last batch entry: %d", batch.logits ? batch.logits[batch.n_tokens - 1] : -1);
        LOGI("- context pointer: %p", inst->ctx);
        LOGI("- sampler pointer: %p", smpl);
        float* logits_ptr = nullptr;
        try {
            logits_ptr = llama_get_logits_ith(inst->ctx, -1);
        } catch (...) {
            LOGE("- llama_get_logits_ith threw an exception!");
        }
        LOGI("- whether llama_get_logits_ith(ctx, -1) returns non-null: %s", (logits_ptr != nullptr) ? "true" : "false");
        LOGI("- vocabulary size: %d", llama_vocab_n_tokens(vocab));

        llama_token id = llama_sampler_sample(smpl, inst->ctx, -1);
        LOGI("Generated token count: %d, token ID: %d", gen_count + 1, id);
        llama_sampler_accept(smpl, id);
        
        if (llama_vocab_is_eog(vocab, id)) {
            LOGI("llama_vocab_is_eog() returned true. EOG reached.");
            break;
        }
        
        char piece_buf[32];
        int32_t pres = llama_token_to_piece(vocab, id, piece_buf, sizeof(piece_buf), 0, false);
        LOGI("llama_token_to_piece() result: %d, buf size limit: %d", pres, (int)sizeof(piece_buf));
        if (pres > 0 && pres < sizeof(piece_buf)) {
            piece_buf[pres] = '\0';
            std::string piece_str(piece_buf);
            
            // Sometimes the model doesn't mark End-Of-Turn correctly in its metadata, 
            // so we manually check if it printed a known stop token as text.
            if (piece_str.find("<|im_end|>") != std::string::npos || 
                piece_str.find("</|im_end|>") != std::string::npos ||
                piece_str.find("<|im_start|>") != std::string::npos ||
                piece_str.find("</|im_start|>") != std::string::npos ||
                piece_str.find("<end_of_turn>") != std::string::npos ||
                piece_str.find("<start_of_turn>") != std::string::npos ||
                piece_str.find("<|end|>") != std::string::npos ||
                piece_str.find("</s>") != std::string::npos) {
                LOGI("Found stop string in piece. Breaking.");
                break;
            }
            
            LOGI("Emitting token to Kotlin: '%s'", piece_buf);
            jstring jstr = env->NewStringUTF(piece_buf);
            env->CallVoidMethod(callback, onTokenMethod, jstr);
            env->DeleteLocalRef(jstr);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
                LOGE("Exception in JNI callback");
                break;
            }
        }
        
        my_llama_batch_clear(batch);
        my_llama_batch_add(batch, id, current_pos, {0}, true);
        
        int loop_decode_res = llama_decode(inst->ctx, batch);
        if (loop_decode_res != 0) {
            LOGE("llama_decode failed during generation with error code: %d", loop_decode_res);
            break;
        }
        
        current_pos++;
        gen_count++;
    }
    LOGI("Generation loop completed.");
    
    int64_t t_end = llama_time_us();
    
    LOGI("Generation metrics:");
    LOGI("Prompt tokens: %d", n_tokens);
    LOGI("Generated tokens: %d", gen_count);
    LOGI("Prompt evaluation time: %lld ms", (long long)(t_first_token - t_start_eval) / 1000);
    LOGI("Time to first token: %lld ms", (long long)(t_first_token - t_start_eval) / 1000);
    LOGI("Total generation time: %lld ms", (long long)(t_end - t_first_token) / 1000);
    if (t_end > t_first_token) {
        double tps = (double)gen_count / ((t_end - t_first_token) / 1000000.0);
        LOGI("Tokens/sec: %.2f", tps);
    }
    
    if (g_cancel_generation) {
        LOGI("Generation cancelled by user.");
    }

    llama_sampler_free(smpl);
    llama_batch_free(batch);
    env->DeleteLocalRef(cbClass);
    LOGI("Native function generateTokens returning.");
}
