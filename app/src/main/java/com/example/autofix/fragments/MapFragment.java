package com.example.autofix.fragments;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autofix.R;
import com.example.autofix.adapters.StationAdapter;
import com.example.autofix.adapters.StationsListAdapter;
import com.example.autofix.bottomsheets.BottomSheetStationFragment;
import com.example.autofix.bottomsheets.BottomSheetStationsList;
import com.example.autofix.data.entities.Station;
import com.example.autofix.sto.BookServiceActivity;
import com.example.autofix.utils.LocationTracker;
import com.example.autofix.viewmodels.CartViewModel;
import com.example.autofix.viewmodels.CartViewModelFactory;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private LocationTracker locationTracker;
    private GoogleMap mMap;
    private TextInputEditText searchEditText;
    private FirebaseFirestore db;
    private RecyclerView stationsRecycler;
    private StationAdapter stationAdapter;

    private CartViewModel cartViewModel;

    private FloatingActionButton fabList, fabZoomIn, fabZoomOut,fabMyLocation;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        locationTracker = new LocationTracker(requireContext());

        cartViewModel = new ViewModelProvider(requireActivity()).get(CartViewModel.class);

        // Инициализация карты
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Инициализация кнопок
        fabList = view.findViewById(R.id.fab_list);
        fabZoomIn = view.findViewById(R.id.fab_zoom_in);
        fabZoomOut = view.findViewById(R.id.fab_zoom_out);
        fabMyLocation = view.findViewById(R.id.fab_my_location);


        db = FirebaseFirestore.getInstance();


        // Настройка поиска
        searchEditText = view.findViewById(R.id.search_edit_text);
        CardView searchCard = view.findViewById(R.id.search_card);

        // Настройка RecyclerView
        stationsRecycler = view.findViewById(R.id.stations_recycler);
        setupStationsRecycler();

        setupButtons();



        return view;
    }

    private void setupButtons() {
        // Кнопка списка станций
        fabList.setOnClickListener(v -> showStationsBottomSheet());

        // Кнопки масштабирования
        fabZoomIn.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });

        fabZoomOut.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });

        fabMyLocation.setOnClickListener(v -> centerMapOnUserLocation());
    }

    private void showStationsBottomSheet() {
        if (stationAdapter == null || stationAdapter.getItemCount() == 0) {
            Toast.makeText(getContext(), "Станции не загружены", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetStationsList bottomSheet = BottomSheetStationsList.newInstance(
                stationAdapter.getOriginalStations(),
                new BottomSheetStationsList.OnStationSelectedListener() {
                    @Override
                    public void onStationSelected(Station station) {
                        moveCameraToStation(station);
                    }

                    @Override
                    public void onBookButtonClicked(Station station) {
                        showBookingDialog(station);
                    }
                }
        );

        bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
    }

    private void setupStationsRecycler() {
        stationsRecycler.setLayoutManager(new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL,
                false));

        // Создаем полную реализацию интерфейса
        StationAdapter.StationClickListener listener = new StationAdapter.StationClickListener() {
            @Override
            public void onStationClick(Station station) {
                // Обработка клика на СТО
                moveCameraToStation(station);
                showBottomSheet(station);
            }

            @Override
            public void onBookButtonClick(Station station) {
                // Обработка клика на кнопку бронирования
                // Можно добавить другую логику или также открыть BottomSheet
                openBookServiceActivity(station);
            }
        };

        stationAdapter = new StationAdapter(new ArrayList<>(), listener);
        stationsRecycler.setAdapter(stationAdapter);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        loadStations();
    }

    private void showBottomSheet(Station station) {
        BottomSheetStationFragment bottomSheet = BottomSheetStationFragment.newInstance(station);
        bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
    }

    private void showBookingDialog(Station station) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Запись на СТО");
        builder.setMessage("Вы хотите записаться на " + station.getStreet() + ", " + station.getBuilding() + "?");

        builder.setPositiveButton("Записаться", (dialog, which) -> {
            cartViewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
                if (cart != null) {
                    cart.setStationId(station.getId());
                    cart.setStationAddress(station.getAddress());
                    cartViewModel.updateCart(cart);
                }
            });

            // Открываем BookServiceActivity
            Intent intent = new Intent(getActivity(), BookServiceActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    // Метод loadStations в MapFragment
    private void loadStations() {
        Log.d("MapFragment", "Starting to load stations");

        db.collection("autoServiceCenters")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Station> stations = new ArrayList<>();
                    Log.d("MapFragment", "Query successful, documents count: " + queryDocumentSnapshots.size());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Log.d("MapFragment", "Processing document: " + document.getId());

                            // Выводим все данные документа для отладки
                            Map<String, Object> data = document.getData();
                            Log.d("MapFragment", "Raw document data: " + data);

                            // Создаем станцию вручную из данных документа
                            Station station = new Station();
                            station.setId(document.getId());

                            // Устанавливаем основные поля
                            station.setCity((String) data.get("city"));
                            station.setStreet((String) data.get("street"));
                            station.setBuilding((String) data.get("building"));
                            station.setWorkingHours((String) data.get("workingHours"));

                            // Устанавливаем списки
                            if (data.containsKey("services")) {
                                station.setServices((List<String>) data.get("services"));
                            }

                            if (data.containsKey("amenities")) {
                                station.setAmenities((List<String>) data.get("amenities"));
                            }

                            if (data.containsKey("photos")) {
                                station.setPhotos((List<String>) data.get("photos"));
                            }


                            // Устанавливаем координаты
                            if (data.containsKey("location")) {
                                GeoPoint location = (GeoPoint) data.get("location");
                                station.setLocation(location);
                                Log.d("MapFragment", "Set location: " + location.getLatitude() + ", " + location.getLongitude());
                            }

                            stations.add(station);
                            Log.d("MapFragment", "Added station: " + station.getStreet() + ", " + station.getBuilding() +
                                    ", coordinates: " + (station.hasCoordinates() ?
                                    station.getLatitude() + "," + station.getLongitude() : "null"));
                        } catch (Exception e) {
                            Log.e("MapFragment", "Error converting document", e);
                            e.printStackTrace();
                        }
                    }

                    if (stations.isEmpty()) {
                        Log.w("MapFragment", "No stations loaded");
                    } else {
                        Log.d("MapFragment", "Loaded " + stations.size() + " stations");
                    }

                    stationAdapter.updateStations(stations);

                    // Добавляем маркеры на карту только для станций с координатами
                    List<Station> stationsWithCoordinates = stations.stream()
                            .filter(Station::hasCoordinates)
                            .collect(java.util.stream.Collectors.toList());

                    if (stationsWithCoordinates.isEmpty()) {
                        Log.w("MapFragment", "No stations with coordinates to show on map");
                    } else {
                        Log.d("MapFragment", "Showing " + stationsWithCoordinates.size() + " stations on map");
                        addMarkersToMap(stationsWithCoordinates);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MapFragment", "Error loading stations", e);
                });
    }

    private void addMarkersToMap(List<Station> stations) {
        if (mMap == null) return;
        mMap.clear();

        for (Station station : stations) {
            // Проверяем, что координаты не null
            if (station.hasCoordinates()) {
                mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(
                                station.getLatitude(),
                                station.getLongitude()))
                        .title(station.getStreet() + " " + station.getBuilding())
                        .snippet(station.getWorkingHours()));
            } else {
                Log.w("MapFragment", "Station has null coordinates: " + station.getId());
            }
        }
    }

    private void moveCameraToStation(Station station) {
        if (mMap != null && station.hasCoordinates()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(station.getLatitude(), station.getLongitude()),
                    18f));
        } else {
            Log.w("MapFragment", "Cannot move camera: map is null or station has null coordinates");
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Проверяем местоположение при готовности карты
        trackUserLocation();
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        locationTracker.onRequestPermissionsResult(requestCode, permissions, grantResults);
        trackUserLocation();
    }

    private void trackUserLocation() {
        locationTracker.startLocationUpdates(new LocationTracker.LocationListener() {
            @Override
            public void onLocationReceived(Location location) {
                // Центрируем карту на местоположении пользователя
                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));

                // Можно добавить маркер местоположения
                mMap.addMarker(new MarkerOptions()
                        .position(userLatLng)
                        .title("Ваше местоположение"));
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(getContext(),
                        "Для отображения вашего местоположения нужны разрешения",
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onLocationError(String error) {
                Log.e("MapFragment", "Location error: " + error);
            }
        });
    }

    private void performSearch(String query) {
        if (stationAdapter != null) {
            stationAdapter.filter(query.trim());

            // Прокручиваем карту к первому результату (если есть)
            if (!query.isEmpty() && stationAdapter.getItemCount() > 0) {
                Station firstStation = stationAdapter.getStationAt(0);
                if (firstStation != null && firstStation.hasCoordinates()) {
                    moveCameraToStation(firstStation);
                }
            }
        }
    }

    private void openBookServiceActivity(Station station) {
        // Обновляем данные в ViewModel
        cartViewModel.getCart().observe(getViewLifecycleOwner(), cart -> {
            if (cart != null) {
                cart.setStationId(station.getId());
                cart.setStationAddress(station.getAddress());
                cartViewModel.updateCart(cart);
            }
        });

        // Открываем BookServiceActivity
        Intent intent = new Intent(getActivity(), BookServiceActivity.class);
        startActivity(intent);
        requireActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void centerMapOnUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.setOnMyLocationButtonClickListener(() -> {
                    centerMapOnUserLocation();
                    return true;
                });
                mMap.setOnMyLocationClickListener(location -> {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
                });
            }else {
                // Запросить разрешения
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locationTracker.stopLocationUpdates();
    }
}