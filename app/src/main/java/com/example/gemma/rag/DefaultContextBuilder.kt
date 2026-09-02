package com.example.gemma.rag

class DefaultContextBuilder : ContextBuilder {

    override fun buildPrompt(query: String, contextChunks: List<DocumentChunk>): String {
        if (contextChunks.isEmpty()) {
            return query
        }

        val contextBuilder = java.lang.StringBuilder()
        contextBuilder.append("Use the following context to answer the question. If you don't know the answer based on the context, say so.\n\n")
        
        contextBuilder.append("--- CONTEXT ---\n")
        contextChunks.forEachIndexed { index, chunk ->
            val pageInfo = if (chunk.pageNumber != null) "Page ${chunk.pageNumber}" else ""
            val chapterInfo = if (chunk.chapter != null) ", Chapter: ${chunk.chapter}" else ""
            contextBuilder.append("[Source: $pageInfo$chapterInfo]\n${chunk.text}\n\n")
        }
        
        contextBuilder.append("--- QUESTION ---\n")
        contextBuilder.append(query)
        contextBuilder.append("\n\nAnswer: ")
        
        return contextBuilder.toString()
    }
}
