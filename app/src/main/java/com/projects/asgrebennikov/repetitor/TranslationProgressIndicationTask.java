package com.projects.asgrebennikov.repetitor;

import android.widget.ArrayAdapter;

import java.util.TimerTask;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by as.grebennikov on 01.08.18.
 */
class TranslationProgressIndicationTask extends TimerTask {
    public TranslationProgressIndicationTask(WordListItem item,
                                             ArrayAdapter<WordListItem> adapter) {
        item_ = item;
        currentProgressIndicator_ = new String();
        adapter_ = adapter;
    }

    @Override
    public void run() {
        if (currentProgressIndicator_.length() > 4) {
            currentProgressIndicator_ = new String();
            item_.setWordAppendix(currentProgressIndicator_);
            notifyAdapter();
            return;
        }
        currentProgressIndicator_ = currentProgressIndicator_ + ".";
        item_.setWordAppendix(currentProgressIndicator_);
        notifyAdapter();
    }

    @Override
    public boolean cancel() {
        item_.setWordAppendix(new String());
        notifyAdapter();
        return true;
    }

    private void notifyAdapter() {
        Flowable.fromCallable(() -> {
            return new Integer(0);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((Integer i) -> {
                    adapter_.notifyDataSetChanged();
                }, Throwable::printStackTrace);
    }

    private WordListItem item_;
    private String currentProgressIndicator_;
    private ArrayAdapter<WordListItem> adapter_;
}
