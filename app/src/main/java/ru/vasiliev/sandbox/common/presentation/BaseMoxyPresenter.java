package ru.vasiliev.sandbox.common.presentation;

import androidx.annotation.NonNull;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import moxy.MvpPresenter;
import moxy.MvpView;
import timber.log.Timber;

public class BaseMoxyPresenter<View extends MvpView> extends MvpPresenter<View> {

    private CompositeDisposable subscriptions = new CompositeDisposable();

    private CompositeDisposable persistentSubscriptions = new CompositeDisposable();

    protected void addSubscription(@NonNull Disposable subscription) {
        subscriptions.add(subscription);
        Timber.d("addSubscription(" + subscription.toString() + ")");
    }

    protected void addPersistentSubscription(@NonNull Disposable subscription) {
        persistentSubscriptions.add(subscription);
        Timber.d("addPersistentSubscription(" + subscription.toString() + ")");
    }

    public CompositeDisposable getSubscriptions() {
        return subscriptions;
    }

    public CompositeDisposable getPersistentSubscriptions() {
        return persistentSubscriptions;
    }

    private void unsubscribe() {
        Timber.d("unsubscribe()");
        subscriptions.clear();
    }

    private void unsubscribePersistent() {
        Timber.d("unsubscribePersistent()");
        persistentSubscriptions.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unsubscribe();
        unsubscribePersistent();
    }
}