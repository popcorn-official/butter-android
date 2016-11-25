/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package butter.droid.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import javax.inject.Inject;

import butter.droid.MobileButterApplication;
import butter.droid.R;
import butter.droid.adapters.GenreAdapter;
import butter.droid.adapters.decorators.DividerItemDecoration;
import butter.droid.base.manager.provider.ProviderManager;
import butter.droid.base.providers.media.models.Genre;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MediaGenreSelectionFragment extends Fragment {

    @Inject
    ProviderManager providerManager;

    private Context mContext;
    private RecyclerView.LayoutManager mLayoutManager;
    private GenreAdapter mAdapter;
    private Listener mListener;
    private int mSelectedPos = 0;

    @BindView(R.id.progressOverlay)
    LinearLayout mProgressOverlay;
    @BindView(R.id.recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.emptyView)
    TextView mEmptyView;
    @BindView(R.id.progress_textview)
    TextView mProgressTextView;

    public static MediaGenreSelectionFragment newInstance(Listener listener) {
        MediaGenreSelectionFragment frag = new MediaGenreSelectionFragment();
        frag.setListener(listener);
        return frag;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileButterApplication.getAppContext()
                .getComponent()
                .inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mContext = getActivity();

        View v = inflater.inflate(R.layout.fragment_media, container, false);
        ButterKnife.bind(this, v);

        mLayoutManager = new LinearLayoutManager(mContext);
        mRecyclerView.setLayoutManager(mLayoutManager);

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        List<Genre> genreList = providerManager.getCurrentMediaProvider().getGenres();

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(mContext, DividerItemDecoration.VERTICAL_LIST, R.drawable.list_divider_nospacing));

        //adapter should only ever be created once on fragment initialise.
        mAdapter = new GenreAdapter(mContext, genreList, mSelectedPos);
        mAdapter.setOnItemSelectionListener(mOnItemSelectionListener);
        mRecyclerView.setAdapter(mAdapter);
    }

    private GenreAdapter.OnItemSelectionListener mOnItemSelectionListener = new GenreAdapter.OnItemSelectionListener() {
        @Override
        public void onItemSelect(View v, Genre item, int position) {
            mSelectedPos = position;
            if (mListener != null)
                mListener.onGenreSelected(item.getKey());
        }
    };

    public interface Listener {
        void onGenreSelected(String genre);
    }

}
