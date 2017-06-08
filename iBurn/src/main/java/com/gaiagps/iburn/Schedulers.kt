package com.gaiagps.iburn

import io.reactivex.schedulers.Schedulers
import java.util.concurrent.Executors

/**
 * Created by dbro on 6/7/17.
 */
// TODO : Base on number of available cores
val ioScheduler = Schedulers.from(Executors.newFixedThreadPool(8))
