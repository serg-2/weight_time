package com.example.weight_time;

import static com.example.weight_time.Constants.NUMBER_OF_TILES_SHARED;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weight_time.models.MainViewModel;
import com.example.weight_time.sharedPreferences.SharedPreference;

// Extends the Adapter class to RecyclerView.Adapter
// and implement the unimplemented methods
public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    private final Context context;
    private final MainViewModel viewModel;
    private final SharedPreference sharedPreference;

    public Adapter(Context context, MainViewModel viewModel, SharedPreference sharedPreference) {
        this.context = context;
        this.viewModel = viewModel;
        this.sharedPreference = sharedPreference;
    }

    @NonNull
    @Override
    public Adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflating the Layout(Instantiates list_item.xml layout file into View object)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_item, parent, false);
        return new ViewHolder(view);
    }

    // Binding data to the into specified position
    @Override
    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position) {
        int item = viewModel.get(position);
        holder.imageViewOfOneTile.setImageResource(item);
    }

    @Override
    public int getItemCount() {
        // Returns number of items currently available in Adapter
        return viewModel.size();
    }

    // Initializing the Views
    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewOfOneTile;

        public ViewHolder(View view) {
            super(view);
            imageViewOfOneTile = view.findViewById(R.id.caloriesElementView);
            // Set click listener on the ViewHolder's item view
            itemView.setOnClickListener(this::onClick);
        }

        private void onClick(View v) {
            int position = getAdapterPosition();
            // Log.e("MAIN", "CLICK " + position);
            viewModel.remove(position);
            notifyItemRemoved(position);
            sharedPreference.save(NUMBER_OF_TILES_SHARED, getItemCount());
        }
    }

    // Reinit adapter by time
    public void reinitAdapter() {
        viewModel.resetCalories();
        notifyDataSetChanged();
        sharedPreference.save(NUMBER_OF_TILES_SHARED, getItemCount());
    }

    public void someOutput() {
        Log.e("CALLBACK", "TEST!");
    }

}
