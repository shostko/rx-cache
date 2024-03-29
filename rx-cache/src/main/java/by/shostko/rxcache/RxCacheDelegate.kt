@file:Suppress("unused", "MoveLambdaOutsideParentheses")

package by.shostko.rxcache

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import io.reactivex.*
import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun <T> Flowable<T>.cache(lifecycle: Lifecycle): Flowable<T> = FlowableCache(lifecycle, { this }).getValue()

fun <T> Flowable<T>.cache(lifecycleOwner: LifecycleOwner): Flowable<T> = FlowableCache(lifecycleOwner.lifecycle, { this }).getValue()

fun <T> Observable<T>.cache(lifecycle: Lifecycle): Observable<T> = ObservableCache(lifecycle, { this }).getValue()

fun <T> Observable<T>.cache(lifecycleOwner: LifecycleOwner): Observable<T> = ObservableCache(lifecycleOwner.lifecycle, { this }).getValue()

fun <T> Single<T>.cache(lifecycle: Lifecycle): Single<T> = SingleCache(lifecycle, { this }).getValue()

fun <T> Single<T>.cache(lifecycleOwner: LifecycleOwner): Single<T> = SingleCache(lifecycleOwner.lifecycle, { this }).getValue()

fun <T> Maybe<T>.cache(lifecycle: Lifecycle): Maybe<T> = MaybeCache(lifecycle, { this }).getValue()

fun <T> Maybe<T>.cache(lifecycleOwner: LifecycleOwner): Maybe<T> = MaybeCache(lifecycleOwner.lifecycle, { this }).getValue()

fun Completable.cache(lifecycle: Lifecycle): Completable = CompletableCache(lifecycle, { this }).getValue()

fun Completable.cache(lifecycleOwner: LifecycleOwner): Completable = CompletableCache(lifecycleOwner.lifecycle, { this }).getValue()

fun <T> LifecycleOwner.flowable(initializer: () -> Flowable<T>): ReadOnlyProperty<LifecycleOwner, Flowable<T>> = FlowableCache(lifecycle, initializer)

fun <T> Lifecycle.flowable(initializer: () -> Flowable<T>): ReadOnlyProperty<LifecycleOwner, Flowable<T>> = FlowableCache(this, initializer)

fun <T> LifecycleOwner.observable(initializer: () -> Observable<T>): ReadOnlyProperty<LifecycleOwner, Observable<T>> = ObservableCache(lifecycle, initializer)

fun <T> Lifecycle.observable(initializer: () -> Observable<T>): ReadOnlyProperty<LifecycleOwner, Observable<T>> = ObservableCache(this, initializer)

fun <T> LifecycleOwner.single(initializer: () -> Single<T>): ReadOnlyProperty<LifecycleOwner, Single<T>> = SingleCache(lifecycle, initializer)

fun <T> Lifecycle.single(initializer: () -> Single<T>): ReadOnlyProperty<LifecycleOwner, Single<T>> = SingleCache(this, initializer)

fun <T> LifecycleOwner.maybe(initializer: () -> Maybe<T>): ReadOnlyProperty<LifecycleOwner, Maybe<T>> = MaybeCache(lifecycle, initializer)

fun <T> Lifecycle.maybe(initializer: () -> Maybe<T>): ReadOnlyProperty<LifecycleOwner, Maybe<T>> = MaybeCache(this, initializer)

fun LifecycleOwner.completable(initializer: () -> Completable): ReadOnlyProperty<LifecycleOwner, Completable> = CompletableCache(lifecycle, initializer)

fun Lifecycle.completable(initializer: () -> Completable): ReadOnlyProperty<LifecycleOwner, Completable> = CompletableCache(this, initializer)

class FlowableCache<T>(lifecycle: Lifecycle, initializer: () -> Flowable<T>) : RxCache<Flowable<T>, FlowableProcessor<T>>(lifecycle, initializer) {

    override fun createHolder(): FlowableProcessor<T> = BehaviorProcessor.create()

    override fun subscribe(upstream: Flowable<T>, holder: FlowableProcessor<T>): Disposable = upstream
            .subscribeOn(Schedulers.io())
            .subscribe(holder::onNext, holder::onError, holder::onComplete)

    override fun finalize(holder: FlowableProcessor<T>): Flowable<T> = holder.hide()
}

class ObservableCache<T>(lifecycle: Lifecycle, initializer: () -> Observable<T>) : RxCache<Observable<T>, Subject<T>>(lifecycle, initializer) {

    override fun createHolder(): Subject<T> = BehaviorSubject.create()

    override fun subscribe(upstream: Observable<T>, holder: Subject<T>): Disposable = upstream
            .subscribeOn(Schedulers.io())
            .subscribe(holder::onNext, holder::onError, holder::onComplete)

    override fun finalize(holder: Subject<T>): Observable<T> = holder.hide()
}

class SingleCache<T>(lifecycle: Lifecycle, initializer: () -> Single<T>) : RxCache<Single<T>, Subject<T>>(lifecycle, initializer) {

    override fun createHolder(): Subject<T> = BehaviorSubject.create()

    override fun subscribe(upstream: Single<T>, holder: Subject<T>): Disposable = upstream
            .subscribeOn(Schedulers.io())
            .subscribe(holder::onNext, holder::onError)

    override fun finalize(holder: Subject<T>): Single<T> = holder.firstOrError()
}

class MaybeCache<T>(lifecycle: Lifecycle, initializer: () -> Maybe<T>) : RxCache<Maybe<T>, Subject<T>>(lifecycle, initializer) {

    override fun createHolder(): Subject<T> = BehaviorSubject.create()

    override fun subscribe(upstream: Maybe<T>, holder: Subject<T>): Disposable = upstream
            .subscribeOn(Schedulers.io())
            .subscribe(holder::onNext, holder::onError, holder::onComplete)

    override fun finalize(holder: Subject<T>): Maybe<T> = holder.firstElement()
}

class CompletableCache(lifecycle: Lifecycle, initializer: () -> Completable) : RxCache<Completable, Subject<Unit>>(lifecycle, initializer) {

    override fun createHolder(): Subject<Unit> = BehaviorSubject.create()

    override fun subscribe(upstream: Completable, holder: Subject<Unit>): Disposable = upstream
            .subscribeOn(Schedulers.io())
            .subscribe(holder::onComplete, holder::onError)

    override fun finalize(holder: Subject<Unit>): Completable = holder.ignoreElements()
}

abstract class RxCache<T, H>(lifecycle: Lifecycle, initializer: () -> T) : ReadOnlyProperty<Any, T> {

    private var lifecycle: Lifecycle? = lifecycle
    private var initializer: (() -> T)? = initializer
    private var holder: H? = null

    internal fun getValue(): T {
        if (holder == null) {
            synchronized(this) {
                if (holder == null) {
                    if (lifecycle!!.currentState == Lifecycle.State.DESTROYED) {
                        throw UnsupportedOperationException("Can't create Stream in DESTROYED state")
                    }
                    holder = createHolder()
                    val disposable = subscribe(initialize(), holder!!)
                    lifecycle!!.addObserver(object : LifecycleEventObserver {
                        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                            if (event == Lifecycle.Event.ON_DESTROY && !disposable.isDisposed) {
                                disposable.dispose()
                            }
                        }
                    })
                    lifecycle = null
                }
            }
        }
        return finalize(holder!!)
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T = getValue()

    private fun initialize(): T {
        val result = initializer!!()
        initializer = null
        return result
    }

    protected abstract fun createHolder(): H

    protected abstract fun subscribe(upstream: T, holder: H): Disposable

    protected abstract fun finalize(holder: H): T
}