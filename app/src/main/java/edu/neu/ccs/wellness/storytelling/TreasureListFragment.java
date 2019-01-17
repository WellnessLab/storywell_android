package edu.neu.ccs.wellness.storytelling;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.util.List;

import edu.neu.ccs.wellness.reflection.ResponsePile;
import edu.neu.ccs.wellness.storytelling.utils.TreasureItemAdapter;
import edu.neu.ccs.wellness.storytelling.viewmodel.TreasureListViewModel;


public class TreasureListFragment extends Fragment {

    public static TreasureListFragment newInstance(){
        return new TreasureListFragment();
    }
    private GridView gridview;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_treasure_list, container, false);
        this.gridview = rootView.findViewById(R.id.gridview);

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TreasureListViewModel viewModel = ViewModelProviders.of(this)
                .get(TreasureListViewModel.class);

        LiveData<List<ResponsePile>> liveData = viewModel.getTreasureListLiveData();
        liveData.observe(this, new Observer<List<ResponsePile>>() {
            @Override
            public void onChanged(@Nullable List<ResponsePile> dataSnapshot) {
                if (dataSnapshot != null) {
                    gridview.setAdapter(new TreasureItemAdapter(getContext(), dataSnapshot));
                    Log.d("SWELL", dataSnapshot.toString());
                }
            }
        });
    }

    // PRIVATE METHODS

    // PRIVATE ASYNCTASK CLASSES
}