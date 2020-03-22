package flavio.com.stayfit;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IFillFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.dataprovider.LineDataProvider;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class WeightTrackerFragment extends Fragment{

    private OnFragmentInteractionListener mListener;

    private GoogleSignInAccount account;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private FirebaseUser user;

    private List<QueryDocumentSnapshot> weightsList;

    String token;



    public WeightTrackerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_weight_tracker, container, false);
        MainActivity a = (MainActivity)getActivity();
        account = a.signIn();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (account != null) {
            if(account.getIdToken()!= null) {
                token = account.getIdToken();
            }
        }

        if(token!=null)
            firebaseAuthWithGoogle(account, view);

        db = FirebaseFirestore.getInstance();

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }


    public void firebaseAuthWithGoogle(final GoogleSignInAccount acct, final View view) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete( Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(MainActivity.CURRENT_TAG, "signInWithCredential:success");
                            user = mAuth.getCurrentUser();
                            firestoreOperations(view, acct, user);
                            db.collection(acct.getId().toString()).addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot snapshot,
                                                    @Nullable FirebaseFirestoreException e) {
                                    if (e != null) {
                                        Log.w(TAG, "Listen failed.", e);
                                        return;
                                    }

                                    firestoreOperations(view, acct, user);
                                }
                            });

                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            MainActivity a = (MainActivity)getActivity();
                            a.signOut();
                            account = a.signIn();
                            Toast.makeText(view.getContext(), "Problem connecting to datasource", Toast.LENGTH_SHORT).show();
                            firebaseAuthWithGoogle(account, view);
                            //firestoreOperations(view, acct, user);
                        }
                    }
                });
    }

    public void firestoreOperations(final View view, final GoogleSignInAccount acct, final FirebaseUser user){

        db.collection(acct.getId().toString() + "-weight").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    weightsList = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        weightsList.add(document);
                    }

                    FloatingActionButton fabAdd = view.findViewById(R.id.fabAdd);
                    fabAdd.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Dialog dialog = new Dialog(view.getContext(),R.style.mydialog);
                            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            dialog.setContentView(R.layout.dialog_add_weight);
                            dialog.setCancelable(true);
                            dialog.setCanceledOnTouchOutside(true);

                            Button btnConfirm = dialog.findViewById(R.id.btnSave);
                            btnConfirm.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    try {
                                        EditText txtWeight = dialog.findViewById(R.id.txtWeight);
                                        if (TextUtils.isEmpty(txtWeight.getText())) {
                                            txtWeight.setError("Weight is required!");
                                        } else {
                                            Map<String, Object> weight = new HashMap<>();
                                            weight.put("weight", Double.parseDouble(txtWeight.getText().toString()));
                                            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                            String date = df.format(Calendar.getInstance().getTime());
                                            weight.put("datetime", date);
                                            db.collection(acct.getId() + "-weight").document()
                                                    .set(weight)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Log.d(TAG, "DocumentSnapshot successfully written!");
                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.w(TAG, "Error writing document", e);
                                                            if (!isNetworkAvailable(view.getContext())) {
                                                                Toast.makeText(view.getContext(), "Data will be uploaded as soon as network connection is available", Toast.LENGTH_LONG).show();
                                                            }
                                                        }
                                                    });

                                            firestoreOperations(view, acct, user);
                                        }
                                    } catch(Exception e){
                                        e.printStackTrace();
                                    }
                                    dialog.dismiss();
                                }
                            });

                            Button btnCancel = dialog.findViewById(R.id.btnCancel);
                            btnCancel.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    dialog.dismiss();
                                }
                            });

                            dialog.show();
                            Window window = dialog.getWindow();
                            window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                            //noinspection deprecation
                            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

                        }
                    });

                    try {
                        setupGraphView(view, weightsList);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    private LineChart chart;
    private SeekBar seekBarX, seekBarY;
    private TextView tvX, tvY;


    /*private void setupGraph(View view, List<QueryDocumentSnapshot> weightsList){
        seekBarX = view.findViewById(R.id.seekBar1);
        seekBarX.setOnSeekBarChangeListener(this);

        seekBarY = view.findViewById(R.id.seekBar2);
        Calendar c = Calendar.getInstance();
        c.add(Calendar.YEAR, 1);
        //seekBarY.setMax(c.getTimeInMillis());
        seekBarY.setOnSeekBarChangeListener(this);

        chart = view.findViewById(R.id.chart1);

        XAxis xAxis;
        xAxis = chart.getXAxis();

        YAxis yAxis;
        yAxis = chart.getAxisLeft();

        // disable dual axis (only use LEFT axis)
        chart.getAxisRight().setEnabled(true);

        // horizontal grid lines
        yAxis.enableGridDashedLine(10f, 10f, 0f);

        // axis range
        yAxis.setAxisMaximum(150f);
        yAxis.setAxisMinimum(0f);

        // add data
        seekBarX.setProgress(40);
        seekBarY.setProgress(180);
        setData(45, 180, weightsList);

        // draw points over time
        //chart.animateX(500);

    }

    private void setData(int count, float range, List<QueryDocumentSnapshot> weightsList) {

        ArrayList<Entry> values = new ArrayList<>();

        for (QueryDocumentSnapshot document : weightsList) {
            Entry e = new Entry();
            e.setX(Float.parseFloat(document.get("datetime").toString()));
            e.setY(Float.parseFloat(document.get("weight").toString()));
            values.add(e);
        }
        LineDataSet set1;
        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
            set1 = (LineDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            set1.notifyDataSetChanged();
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            // create a dataset and give it a type
            set1 = new LineDataSet(values, "DataSet 1");

            set1.setDrawIcons(false);

            // draw dashed line
            set1.enableDashedLine(10f, 5f, 0f);

            // black lines and points
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);

            // line thickness and point size
            set1.setLineWidth(1f);
            set1.setCircleRadius(3f);

            // draw points as solid circles
            set1.setDrawCircleHole(true);

            // customize legend entry
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

            // text size of values
            set1.setValueTextSize(9f);

            // draw selection line as dashed
            set1.enableDashedHighlightLine(10f, 5f, 0f);

            // set the filled area
            set1.setDrawFilled(true);
            set1.setFillFormatter(new IFillFormatter() {
                @Override
                public float getFillLinePosition(ILineDataSet dataSet, LineDataProvider dataProvider) {
                    return chart.getAxisLeft().getAxisMinimum();
                }
            });

            // set color of filled area
            if (Utils.getSDKInt() >= 18) {
                // drawables only supported on api level 18 and above
                Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.fade_red);
                set1.setFillDrawable(drawable);
            } else {
                set1.setFillColor(Color.BLACK);
            }

            ArrayList<ILineDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1); // add the data sets

            // create a data object with the data sets
            LineData data = new LineData(dataSets);

            // set data
            chart.setData(data);
        }
    }
*/

    private void setupGraphView(final View v, List<QueryDocumentSnapshot> weightsList ) throws ParseException {
        GraphView graph = (GraphView)v.findViewById(R.id.graph);

        final List<DataPoint> dataPoints = new ArrayList<>();
        int x = 0;

        Collections.sort(weightsList, new Comparator<QueryDocumentSnapshot>(){
            public int compare(QueryDocumentSnapshot obj1, QueryDocumentSnapshot obj2) {
                // ## Ascending order
                //return obj1.firstName.compareToIgnoreCase(obj2.firstName); // To compare string values
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                int result = -1;
                try {
                    Date d1 = df.parse(obj1.get("datetime").toString());
                    Date d2 = df.parse(obj2.get("datetime").toString());

                    result = d1.compareTo(d2);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return result;
                // To compare integer values

                // ## Descending order
                // return obj2.firstName.compareToIgnoreCase(obj1.firstName); // To compare string values
                // return Integer.valueOf(obj2.empId).compareTo(Integer.valueOf(obj1.empId)); // To compare integer values
            }
        });
        for (QueryDocumentSnapshot document : weightsList) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.d(WeightTrackerFragment.class.getName(),x + " - " + document.get("datetime").toString());
            DataPoint p = new DataPoint(df.parse(document.get("datetime").toString()),Double.parseDouble(document.get("weight").toString()));
            dataPoints.add(p);
            x++;
        }
        DataPoint[] points = new DataPoint[dataPoints.size()];
        dataPoints.toArray(points);


        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>((DataPoint[]) points);
        series.setDrawDataPoints(true);
        series.setDrawBackground(true);
        series.setAnimated(true);
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                double y = dataPoint.getY();
                Dialog dialog = new Dialog(v.getContext());
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.dialog_datapoint_tap);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                TextView txtWeight = dialog.findViewById(R.id.txtWeight);
                txtWeight.setText(y+" Kg");
                ImageButton delete = dialog.findViewById(R.id.removeInstance);
                delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                });
                dialog.show();
            }
        });
        // styling series
        series.setColor(Color.GREEN);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(20);
        series.setThickness(5);

        graph.getViewport().setDrawBorder(true);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(30);
        graph.getViewport().setMaxY(140);
        graph.getViewport().setXAxisBoundsManual(true);
        if(points.length>5)
            graph.getViewport().setMinX(points[(points.length-1)-5].getX());
        else
            graph.getViewport().setMinX(points[0].getX());

        graph.getViewport().setMaxX(points[points.length-1].getX());

        graph.getViewport().setScrollable(true);

        graph.getGridLabelRenderer().setVerticalAxisTitle("Kg");
        graph.getGridLabelRenderer().setVerticalAxisTitleColor(Color.BLACK);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(true);
        graph.getGridLabelRenderer().setGridColor(Color.GRAY);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.BLACK);
        graph.getGridLabelRenderer().setNumVerticalLabels(22);
        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX){
                DateFormat df = new SimpleDateFormat("yyyy\nMM\ndd");
                if(isValueX){
                    return df.format(new Date((long) value));
                }else {
                    return super.formatLabel(value, isValueX);
                }
            }
        });

        graph.addSeries(series);
    }


    /*@Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        setData(seekBarX.getProgress(), seekBarY.getProgress(), weightsList);

        // redraw
        chart.invalidate();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
*/
    private boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(ctx.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
