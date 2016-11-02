package butter.droid.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.Bind;
import butter.droid.R;
import butter.droid.base.providers.media.models.Episode;

public class EpisodeListAdapter extends BaseAdapter {

    private List<Episode> mData;
    private LayoutInflater mInflater;
    private int mColor = 0x0;

    static class ViewHolder {
        public ViewHolder(View v) {
            ButterKnife.bind(this, v);
        }

        @Bind(R.id.info)
        TextView number;
        @Bind(R.id.title)
        TextView title;
    }

    public EpisodeListAdapter(LayoutInflater inflater, List<Episode> data, int color) {
        mData = data;
        mInflater = inflater;
        mColor = color;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Episode getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.episode_list_item, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Episode episode = getItem(position);
        if (!TextUtils.isEmpty(episode.title))
            holder.title.setText(episode.title);
        else
            holder.title.setText(R.string.no_title_available);

        holder.number.setText("E" + episode.episode);
        holder.number.setTextColor(mColor);

        return convertView;
    }

}
