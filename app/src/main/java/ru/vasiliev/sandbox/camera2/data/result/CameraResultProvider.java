package ru.vasiliev.sandbox.camera2.data.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import ru.vasiliev.sandbox.camera2.data.action.CameraAction;
import ru.vasiliev.sandbox.camera2.data.action.CameraActionKind;

public class CameraResultProvider {

    private Map<String, CameraResult> storage = new HashMap<>();

    private PublishSubject<CameraResult> publisher = PublishSubject.create();

    private CameraResultProvider() {
    }

    public static CameraResultProvider getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private String createHashKey(CameraActionKind kind,
                                 long captureId,
                                 long scanId,
                                 long index) {
        return String.format(Locale.getDefault(), "%s:%d:%d:%d", kind.getCode(), captureId, scanId, index);
    }

    public void put(CameraResult cameraResult) {
        storage.put(cameraResult.getAction()
                                .getHashKey(), cameraResult);
        publisher.onNext(cameraResult);
    }

    public Observable<CameraResult> getResultObservable() {
        return publisher;
    }

    public boolean contains(CameraAction cameraAction) {
        return storage.containsKey(cameraAction.getHashKey());
    }

    public boolean contains(String actionHashKey) {
        return storage.containsKey(actionHashKey);
    }

    public boolean contains(CameraActionKind kind,
                            long captureId,
                            long scanId,
                            long index) {
        return contains(createHashKey(kind, captureId, scanId, index));
    }

    public CameraResult get(String actionHashKey) {
        return storage.get(actionHashKey);
    }

    public CameraResult get(CameraActionKind kind,
                            long captureId,
                            long scanId,
                            long index) {
        return get(createHashKey(kind, captureId, scanId, index));
    }

    public List<CameraResult> getAll() {
        List<CameraResult> resultList = new ArrayList<>();
        for (Map.Entry<String, CameraResult> entry : storage.entrySet()) {
            resultList.add(entry.getValue());
        }
        Collections.sort(resultList);
        return resultList;
    }

    public List<CameraResult> getAll(CameraActionKind kind) {
        List<CameraResult> resultList = new ArrayList<>();
        for (Map.Entry<String, CameraResult> entry : storage.entrySet()) {
            CameraResult result = entry.getValue();
            if (result.getAction()
                      .getKind() == kind) {
                resultList.add(result);
            }
        }
        Collections.sort(resultList);
        return resultList;
    }

    public CameraResult searchBarcode(String barcode) {
        for (Map.Entry<String, CameraResult> entry : storage.entrySet()) {
            CameraResult result = entry.getValue();
            if (result.hasBarcode() && result.getBarcode()
                                             .equals(barcode)) {
                return result;
            }
        }
        return null;
    }

    public void discard(String actionHashKey) {
        if (actionHashKey == null || ("").equals(actionHashKey)) {
            return;
        }
        Iterator<Map.Entry<String, CameraResult>> iter = storage.entrySet()
                                                                .iterator();
        while (iter.hasNext()) {
            Map.Entry<String, CameraResult> entry = iter.next();
            if (actionHashKey.equals(entry.getValue()
                                          .getAction()
                                          .getHashKey())) {
                iter.remove();
            }
        }
    }

    public void clear() {
        storage.clear();
    }

    private static class InstanceHolder {
        static final CameraResultProvider INSTANCE = new CameraResultProvider();
    }
}