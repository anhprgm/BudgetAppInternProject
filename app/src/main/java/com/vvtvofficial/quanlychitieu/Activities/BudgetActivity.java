package com.vvtvofficial.quanlychitieu.Activities;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.vvtvofficial.quanlychitieu.DataBase.Data;
import com.vvtvofficial.quanlychitieu.R;

import org.joda.time.DateTime;
import org.joda.time.Months;
import org.joda.time.MutableDateTime;
import org.joda.time.Weeks;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class BudgetActivity extends AppCompatActivity {
    private FloatingActionButton fab;
    private TextView totalBudgetAmountTextView;
    private RecyclerView recyclerView;
    private DatabaseReference budgetRef;
    private ProgressDialog loader;


    private String postKey = "";
    private String item = "";
    private int amount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        budgetRef = FirebaseDatabase.getInstance().getReference().child("budget").child(Objects.requireNonNull(mAuth.getCurrentUser()).getUid());
        loader = new ProgressDialog(this);


        budgetRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalAmount = 0;
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Data data = snap.getValue(Data.class);
                    assert data != null;
                    if (!data.getItem().equals("Thu Nh???p")) {
                        totalAmount += data.getAmount();
                    }
                    String sTotal = "Chi Ti??u: " + dotMoney(totalAmount) + "VN??";
                    totalBudgetAmountTextView.setText(sTotal);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        binding();
        fab.setOnClickListener(v -> addItem());
    }

    private void binding() {
        fab = findViewById(R.id.fab);
        recyclerView = findViewById(R.id.recyclerViewBudget);
        totalBudgetAmountTextView = findViewById(R.id.totalBudgetAmountTextView);

    }

    private void addItem() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View myView = inflater.inflate(R.layout.input_layout, null);
        myDialog.setView(myView);

        final AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);

        final Spinner itemSpinner = myView.findViewById(R.id.itemsSpinner);
        final EditText amount = myView.findViewById(R.id.amount);
        final TextView cancel = myView.findViewById(R.id.cancel);
        final TextView save = myView.findViewById(R.id.save);
        final EditText notes = myView.findViewById(R.id.note);
        save.setOnClickListener(v -> {
            String budgetAmount = amount.getText().toString().trim();
            String budgetItem = itemSpinner.getSelectedItem().toString();
            String note = notes.getText().toString();
            if (TextUtils.isEmpty(note)) {
                note = null;
            }
            if (TextUtils.isEmpty(budgetAmount)) {
                amount.setError("Nh???p Kh??c 0");
                return;
            }

            if (budgetItem.equals("Select")) {
                Toast.makeText(BudgetActivity.this, "Ch???n M???c H???p L???", Toast.LENGTH_SHORT).show();
            } else {
                loader.setMessage("??ang th??m m???t m???c t??i ch??nh");
                loader.setCanceledOnTouchOutside(false);
                loader.show();

                String id = budgetRef.push().getKey();
                DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                Calendar cal = Calendar.getInstance();
                String date = dateFormat.format(cal.getTime());

                MutableDateTime epoch = new MutableDateTime();
                epoch.setDate(0);
                DateTime now = new DateTime();
                Months month = Months.monthsBetween(epoch, now);
                Weeks weeks = Weeks.weeksBetween(epoch, now);
                Data data = new Data(budgetItem, date, id, note, Integer.parseInt(budgetAmount), month.getMonths(), weeks.getWeeks());
                assert id != null;
                budgetRef.child(id).setValue(data).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(BudgetActivity.this, "Th??m Th??nh C??ng", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(BudgetActivity.this, Objects.requireNonNull(task.getException()).toString(), Toast.LENGTH_SHORT).show();
                    }

                    loader.dismiss();
                });
                dialog.dismiss();
            }
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Data> options = new FirebaseRecyclerOptions.Builder<Data>()
                .setQuery(budgetRef, Data.class)
                .build();
        FirebaseRecyclerAdapter<Data, MyViewHolder> adapter = new FirebaseRecyclerAdapter<Data, MyViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull MyViewHolder holder, int position, @NonNull Data model) {
                holder.setDate("Th???i gian: " + model.getData());
                if (Objects.equals(model.getItem(), "Thu Nh???p")) {
                    holder.setItemAmountGreen("Thu Nh???p: " + dotMoney(model.getAmount()) + "??");
                    holder.setItemName( model.getItem());
                }else {
                    holder.setItemAmount("Ti??u: " + dotMoney(model.getAmount()) + "??");
                    holder.setItemName("Ti??u Cho: " + model.getItem());
                }
                if (model.getNotes() == null) {
                    holder.notes.setVisibility(View.GONE);
                } else {
                    holder.notes.setVisibility(View.VISIBLE);
                    holder.setNotes("Note: " + model.getNotes());
                }

                switch (model.getItem()) {
                    case "Di Chuy???n":
                        holder.imageView.setImageResource(R.drawable.ic_transport);
                        break;
                    case "??n U???ng":
                        holder.imageView.setImageResource(R.drawable.ic_eat);
                        break;
                    case "Nh?? C???a":
                        holder.imageView.setImageResource(R.drawable.ic_home);
                        break;
                    case "Gi???i tr??":
                        holder.imageView.setImageResource(R.drawable.ic_entertainment);
                        break;
                    case "H???c T???p":
                        holder.imageView.setImageResource(R.drawable.ic_study);
                        break;
                    case "T??? Thi???n":
                        holder.imageView.setImageResource(R.drawable.ic_charity);
                        break;
                    case "S???c Kh???e":
                        holder.imageView.setImageResource(R.drawable.ic_health);
                        break;
                    case "C?? Nh??n":
                        holder.imageView.setImageResource(R.drawable.ic_clothes);
                        break;
                    case "Thu Nh???p":
                        holder.imageView.setImageResource(R.drawable.ic_income);
                        break;
                    case "Kh??c":
                        holder.imageView.setImageResource(R.drawable.ic_other);
                        break;
                }

                holder.mView.setOnLongClickListener(v -> {
                    postKey = getRef(position).getKey();
                    item = model.getItem();
                    amount = model.getAmount();
                    updateData();
                    return false;
                });
            }

            @NonNull
            @Override
            public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.retrieve_layout, parent, false);
                return new MyViewHolder(view);
            }
        };
        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        View mView;
        public ImageView imageView;
        public TextView notes;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            imageView = itemView.findViewById(R.id.imageView);
            notes = itemView.findViewById(R.id.note);


        }

        public void setItemName(String itemName) {
            TextView item = mView.findViewById(R.id.item);
            item.setText(itemName);
        }

        public void setItemAmount(String itemAmount) {
            TextView amount = mView.findViewById(R.id.amount);
            amount.setText(itemAmount);
        }
        public void setItemAmountGreen(String itemAmount) {
            TextView amount = mView.findViewById(R.id.amount);
            amount.setText(itemAmount);
            amount.setTextColor(Color.rgb(0, 121, 107));
        }
        public void setDate(String itemDate) {
            TextView date = mView.findViewById(R.id.date);
            date.setText(itemDate);
        }

        public void setNotes(String notes) {
            TextView note = mView.findViewById(R.id.note);
            note.setText(notes);
        }
    }

    public String dotMoney(int x) {
        StringBuilder xDot = new StringBuilder();
        int count  = 0;
        while (x > 0) {
            xDot.append(x % 10);
            x /= 10;
            count++;
            if (count % 3 == 0) xDot.append(',');
        }
        String vDot = xDot.reverse().toString();
        vDot = vDot.startsWith(",") ? vDot.substring(1) : vDot;
        return vDot;
    }

    private void updateData() {
        AlertDialog.Builder myDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View mview = inflater.inflate(R.layout.update_layout, null);

        myDialog.setView(mview);
        final AlertDialog dialog = myDialog.create();
        dialog.setCancelable(false);

        final TextView mItem = mview.findViewById(R.id.itemName);
        final EditText mAmount = mview.findViewById(R.id.amount);
        final EditText mNotes = mview.findViewById(R.id.note);

        mNotes.setVisibility(View.GONE);

        mItem.setText(item);

        mAmount.setText(String.valueOf(amount));
        mAmount.setSelection(String.valueOf(amount).length());

        TextView delBtn = mview.findViewById(R.id.btnDelete);
        TextView updBtn = mview.findViewById(R.id.btnUpdate);

        updBtn.setOnClickListener(v -> {
            amount = Integer.parseInt(mAmount.getText().toString());

            DateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
            Calendar cal = Calendar.getInstance();
            String date = dateFormat.format(cal.getTime());

            MutableDateTime epoch = new MutableDateTime();
            epoch.setDate(0);
            DateTime now = new DateTime();
            Months month = Months.monthsBetween(epoch, now);
            Weeks weeks = Weeks.weeksBetween(epoch, now);
            Data data = new Data(item, date, postKey, null, amount, month.getMonths(), weeks.getWeeks());
            assert postKey != null;
            budgetRef.child(postKey).setValue(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(BudgetActivity.this, "C???p Nh???t Th??nh C??ng", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BudgetActivity.this, Objects.requireNonNull(task.getException()).toString(), Toast.LENGTH_SHORT).show();
                }

            });
            dialog.dismiss();
        });
        delBtn.setOnClickListener(v -> {
            budgetRef.child(postKey).removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(BudgetActivity.this, "X??a Th??nh C??ng", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(BudgetActivity.this, Objects.requireNonNull(task.getException()).toString(), Toast.LENGTH_SHORT).show();
                }

            });
            dialog.dismiss();
        });

        dialog.show();
    }
}