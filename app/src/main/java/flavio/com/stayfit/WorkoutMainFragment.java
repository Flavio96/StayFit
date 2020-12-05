package flavio.com.stayfit;

import android.app.Dialog;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import flavio.com.stayfit.utils.WorkoutsCustomAdapter;

import static androidx.constraintlayout.widget.Constraints.TAG;

public class WorkoutMainFragment extends Fragment {

    private GoogleSignInAccount account;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser user;
    String token;
    List<Exercise> exercises;
    List<QueryDocumentSnapshot> workoutsList;
    FloatingActionButton fabAddWorkout;

    public WorkoutMainFragment() {
        // Required empty public constructor
    }

    public static WorkoutMainFragment newInstance(String param1, String param2) {
        WorkoutMainFragment fragment = new WorkoutMainFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_workout_main, container, false);

        MainActivity a = (MainActivity)getActivity();
        account = a.signIn();
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        if (account != null) {
            if(account.getIdToken()!= null) {
                token = account.getIdToken();
            }
        }

        fabAddWorkout = view.findViewById(R.id.fabAddWorkout);

        if(token!=null)
            firebaseAuthWithGoogle(account, view);

        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        return view;
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
        db.collection(acct.getId().toString() + "-workouts").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
             @Override
             public void onComplete(@NonNull Task<QuerySnapshot> task) {
                 if (task.isSuccessful()) {
                     final List<QueryDocumentSnapshot> workoutsIds = new ArrayList<>();

                     for (QueryDocumentSnapshot document : task.getResult()) {
                         workoutsIds.add(document);
                     }
                     db.collection("workouts-datasource").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                         @Override
                         public void onComplete(@NonNull Task<QuerySnapshot> task) {
                             if (task.isSuccessful()) {
                                 workoutsList = new ArrayList<>();
                                 List<String> wNames =new ArrayList<>();

                                 for (QueryDocumentSnapshot document : task.getResult()) {
                                     int i;
                                     for(i=0; i<workoutsIds.size(); i++){
                                         Log.w(TAG, document.getId());
                                         if(workoutsIds.get(i).get("wkId").equals(document.getId())) {
                                             workoutsList.add(document);
                                             wNames.add((String) document.get("name"));
                                         }
                                     }
                                     if(workoutsIds.size() == workoutsList.size()){
                                         break;
                                     }
                                 }
                                 ListView l = view.findViewById(R.id.workouts_list);
                                 WorkoutsCustomAdapter wa = new WorkoutsCustomAdapter(wNames, view.getContext());
                                 l.setAdapter(wa);

                                 fabAddWorkout.setOnClickListener(new View.OnClickListener() {
                                     @Override
                                     public void onClick(View v) {
                                         final Dialog dialog = new Dialog(view.getContext(),R.style.mydialog);
                                         dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                         dialog.setContentView(R.layout.dialog_create_workout);
                                         dialog.setCancelable(true);
                                         dialog.setCanceledOnTouchOutside(true);

                                         Button btnConfirm = dialog.findViewById(R.id.btnSave);
                                         btnConfirm.setOnClickListener(new View.OnClickListener() {
                                             @Override
                                             public void onClick(View v) {
                                                 try {
                                                     EditText txtName = dialog.findViewById(R.id.txtWorkoutName);
                                                     if (TextUtils.isEmpty(txtName.getText())) {
                                                         txtName.setError("Name is required!");
                                                     } else {
                                                         Map<String, Object> workout = new HashMap<>();
                                                         workout.put("name", txtName.getText().toString());
                                                         workout.put("sets", 0);
                                                         final DocumentReference workoutDoc = db.collection("workouts-datasource").document();
                                                         workoutDoc.set(workout)
                                                                 .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                     @Override
                                                                     public void onSuccess(Void aVoid) {
                                                                         Log.d(TAG, "DocumentSnapshot successfully written!");

                                                                         Map<String, Object> acctWorkoutRelation = new HashMap<>();
                                                                         acctWorkoutRelation.put("wkId", workoutDoc.getId());
                                                                         db.collection(acct.getId() + "-workouts").document().
                                                                                 set(acctWorkoutRelation).addOnSuccessListener(new OnSuccessListener<Void>() {
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
                             }
                         }
                     });


                 }
             }
         });
    }

    private boolean isNetworkAvailable(Context ctx) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) ctx.getSystemService(ctx.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    /*
    public static List<Exercise> processXMLData(XmlResourceParser parser, List<Exercise> exercises)throws IOException, XmlPullParserException {
        int eventType = -1;
        // Loop through the XML data
        while(eventType!=parser.END_DOCUMENT){
            if(eventType == XmlResourceParser.START_TAG){
                Exercise e = new Exercise();
                String element = parser.getName();
                if(element.equals("exercise")){
                    if(!parser.getAttributeValue(null,"name").isEmpty()) {
                        e.setId(Integer.parseInt(parser.getAttributeValue(null, "id")));
                        e.setName(parser.getAttributeValue(null, "name"));
                        e.setMuscles(parser.getAttributeValue(null, "muscles"));
                        e.setUrl(parser.getAttributeValue(null, "url"));
                        e.setSeconds(Integer.parseInt(parser.getAttributeValue(null, "seconds")));
                        e.setType(parser.getAttributeValue(null, "type"));
                        exercises.add(e);
                    }
                }

            }
            eventType = parser.next();
        }

        return exercises;
    }*/
}
