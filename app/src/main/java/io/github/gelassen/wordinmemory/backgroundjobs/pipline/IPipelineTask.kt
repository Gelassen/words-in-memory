package io.github.gelassen.wordinmemory.backgroundjobs.pipline

interface IPipelineTask {

    /**
     * The origin intent is to use it inside coroutine worker, so it likely will operate with coroutine
     * */
    suspend fun process(): IPipelineTask
}