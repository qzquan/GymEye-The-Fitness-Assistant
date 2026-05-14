package com.example.strong_body;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EquipmentListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equipment_list);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        RecyclerView rv = findViewById(R.id.rvEquipmentList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Equipment> equipmentList = EquipmentRepository.getAllEquipment();
        EquipmentListAdapter adapter = new EquipmentListAdapter(equipmentList);
        adapter.setOnEquipmentClickListener(equipment -> {
            Intent intent = new Intent(this, EquipmentDetailActivity.class);
            intent.putExtra(EquipmentDetailActivity.EXTRA_EQUIPMENT_NAME, equipment.getName());
            startActivity(intent);
        });
        rv.setAdapter(adapter);
    }
}
