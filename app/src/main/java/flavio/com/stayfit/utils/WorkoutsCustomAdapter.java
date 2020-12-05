package flavio.com.stayfit.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

import flavio.com.stayfit.R;

public class WorkoutsCustomAdapter extends ArrayAdapter<String> implements View.OnClickListener{

    private List<String> dataSet;
    Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView txtName, txtType, txtSets;
    }

    public WorkoutsCustomAdapter(List<String> data, Context context) {
        super(context, R.layout.workout_list_element, data);
        this.dataSet = data;
        this.mContext=context;

    }

    @Override
    public void onClick(View v) {

        int position=(Integer) v.getTag();
        Object object= getItem(position);
        Workout dataModel=(Workout)object;

        /*switch (v.getId())
        {
            case R.id.item_info:
                Snackbar.make(v, "Release date " +dataModel.getFeature(), Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
                break;
        }*/
    }

    private int lastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        String dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        ViewHolder viewHolder; // view lookup cache stored in tag

        final View result;

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.workout_list_element, parent, false);
            viewHolder.txtName = (TextView) convertView.findViewById(R.id.w_name);

            result=convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            result=convertView;
        }
        lastPosition = position;

        viewHolder.txtName.setText(dataModel);
        // Return the completed view to render on screen
        return convertView;
    }
}