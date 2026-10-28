package io.github.gelassen.wordinmemory.backgroundjobs.pipline

import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

interface IPipeline {

    val list: Queue<IPipeline>

    fun run(): IPipeline
}