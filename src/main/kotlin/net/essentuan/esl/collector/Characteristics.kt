package net.essentuan.esl.collector

import java.util.stream.Collector

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Characteristics(vararg val value: Collector.Characteristics)
