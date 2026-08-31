# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.
project("gemma")

# Add the llama.cpp subdirectory. We disable tests and executables.
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "Build llama tests" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "Build llama examples" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "Build llama server" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build shared libs" FORCE)

# Disable advanced x86 instructions for emulator compatibility to prevent silent SIGILL crashes
set(LLAMA_AVX OFF CACHE BOOL "llama: use AVX" FORCE)
set(LLAMA_AVX2 OFF CACHE BOOL "llama: use AVX2" FORCE)
set(LLAMA_F16C OFF CACHE BOOL "llama: use F16C" FORCE)
set(LLAMA_FMA OFF CACHE BOOL "llama: use FMA" FORCE)
set(LLAMA_AVX512 OFF CACHE BOOL "llama: use AVX512" FORCE)

add_subdirectory(llama.cpp)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
add_library(
        # Sets the name of the library.
        gemma_jni
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        llama_jni.cpp)

# Specifies libraries CMake should link to your target library.
target_link_libraries(
        gemma_jni
        llama
        android
        log)

# Include paths for llama.cpp and ggml
target_include_directories(gemma_jni PRIVATE
        llama.cpp/include
        llama.cpp/ggml/include
)
