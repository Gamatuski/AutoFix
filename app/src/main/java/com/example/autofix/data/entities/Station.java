package com.example.autofix.data.entities;


import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Locale;

@IgnoreExtraProperties
public class Station implements Parcelable {
    private String phone = "+7 (4212) 72-03-03";
    private String id;
    private String city;
    private String street;
    private String building;
    private List<String> photos;
    private String workingHours;
    private List<String> services;
    private List<String> amenities;
    private GeoPoint location;

    public Station() {}

    public Station(String city, String street, String building, List<String> photos,
                   String workingHours, List<String> services, List<String> amenities,
                   double latitude, double longitude) {
        this.city = city;
        this.street = street;
        this.building = building;
        this.photos = photos;
        this.workingHours = workingHours;
        this.services = services;
        this.amenities = amenities;
        this.location = new GeoPoint(latitude, longitude);
    }

    protected Station(Parcel in) {
        id = in.readString();
        city = in.readString();
        street = in.readString();
        building = in.readString();
        photos = in.createStringArrayList();
        workingHours = in.readString();
        services = in.createStringArrayList();
        amenities = in.createStringArrayList();
    }

    public static final Creator<Station> CREATOR = new Creator<Station>() {
        @Override
        public Station createFromParcel(Parcel in) {
            return new Station(in);
        }

        @Override
        public Station[] newArray(int size) {
            return new Station[size];
        }
    };

    // Геттеры и сеттеры
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }
    public List<String> getPhotos() { return photos; }
    public void setPhotos(List<String> photos) { this.photos = photos; }
    public String getWorkingHours() { return workingHours; }
    public void setWorkingHours(String workingHours) { this.workingHours = workingHours; }
    public List<String> getServices() { return services; }
    public void setServices(List<String> services) { this.services = services; }
    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }
    public GeoPoint getLocation() { return location; }
    public void setLocation(GeoPoint location) { this.location = location; }

    // Методы для работы с записями
    public void checkAvailability(String date, String time, AvailabilityCallback callback) {
        String[] dateParts = date.split("-");
        if (dateParts.length != 3) {
            callback.onComplete(false);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("autoServiceCenters")
                .document(id)
                .collection("appointments")
                .document(dateParts[0]) // год
                .collection("months")
                .document(dateParts[1]) // месяц
                .collection("days")
                .document(dateParts[2]) // день
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Map<String, Boolean> times = (Map<String, Boolean>) task.getResult().get("times");
                        callback.onComplete(times != null && Boolean.TRUE.equals(times.get(time)));
                    } else {
                        callback.onComplete(false);
                    }
                });
    }

    public void bookTimeSlot(String date, String timeSlot, BookingCallback callback) {
        String[] dateParts = date.split("-");
        if (dateParts.length != 3) {
            callback.onComplete(false);
            return;
        }

        String year = dateParts[0];
        String month = dateParts[1];
        String day = dateParts[2];

        FirebaseFirestore.getInstance()
                .collection("autoServiceCenters")
                .document(id)
                .collection("appointments")
                .document(year)
                .collection("months")
                .document(month)
                .collection("days")
                .document(day)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        updateTimeSlot(id, year, month, day, timeSlot, callback);
                    } else {
                        createTimeSlotDocument(id, year, month, day, timeSlot, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Station", "Error checking document existence", e);
                    callback.onComplete(false);
                });
    }

    private void updateTimeSlot(String stationId, String year, String month, String day, String timeSlot, BookingCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("times." + timeSlot, false);

        FirebaseFirestore.getInstance()
                .collection("autoServiceCenters")
                .document(stationId)
                .collection("appointments")
                .document(year)
                .collection("months")
                .document(month)
                .collection("days")
                .document(day)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e("Station", "Error updating time slot", e);
                    callback.onComplete(false);
                });
    }

    private void createTimeSlotDocument(String stationId, String year, String month, String day, String timeSlot, BookingCallback callback) {
        Map<String, Boolean> times = new HashMap<>();
        times.put(timeSlot, false);

        Map<String, Object> data = new HashMap<>();
        data.put("times", times);

        FirebaseFirestore.getInstance()
                .collection("autoServiceCenters")
                .document(stationId)
                .collection("appointments")
                .document(year)
                .collection("months")
                .document(month)
                .collection("days")
                .document(day)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onComplete(true))
                .addOnFailureListener(e -> {
                    Log.e("Station", "Error creating time slot document", e);
                    callback.onComplete(false);
                });
    }

    public void getAvailableTimeSlots(String date, OnTimeSlotsLoadedListener listener) {
        try {
            String[] parts = date.split("-");
            if (parts.length != 3) {
                listener.onTimeSlotsLoaded(new ArrayList<>());
                return;
            }

            String year = parts[0];
            String month = String.valueOf(Integer.parseInt(parts[1]));
            String day = String.valueOf(Integer.parseInt(parts[2]));

            String path = String.format(
                    "autoServiceCenters/%s/appointments/%s/months/%s/days/%s",
                    this.id, year, month, day
            );

            Log.d("Station", "Fetching slots from: " + path);

            FirebaseFirestore.getInstance()
                    .document(path)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Map<String, Boolean> times = (Map<String, Boolean>) documentSnapshot.get("times");
                            List<String> availableSlots = new ArrayList<>();
                            if (times != null) {
                                for (Map.Entry<String, Boolean> entry : times.entrySet()) {
                                    availableSlots.add(entry.getKey());
                                }
                            }
                            listener.onTimeSlotsLoaded(availableSlots);
                        } else {
                            listener.onTimeSlotsLoaded(new ArrayList<>());
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Station", "Error loading slots", e);
                        listener.onTimeSlotsLoaded(new ArrayList<>());
                    });
        } catch (Exception e) {
            Log.e("Station", "Date parsing error", e);
            listener.onTimeSlotsLoaded(new ArrayList<>());
        }
    }



    // Вспомогательные методы
    public String getAddress() {
        return city + ", " + street + ", " + building;
    }

    public boolean hasService(String serviceName) {
        return services != null && services.contains(serviceName);
    }

    public boolean hasAmenity(String amenityName) {
        return amenities != null && amenities.contains(amenityName);
    }

    public Map<String, String> getWorkingHourRange() {
        Map<String, String> result = new HashMap<>();
        if (workingHours != null && workingHours.contains("-")) {
            String[] hours = workingHours.split("-");
            result.put("opening", hours[0].trim());
            result.put("closing", hours[1].trim());
        }
        return result;
    }

    public double getLatitude() {
        return location != null ? location.getLatitude() : 0;
    }

    public double getLongitude() {
        return location != null ? location.getLongitude() : 0;
    }

    public boolean hasCoordinates() {
        return location != null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(city);
        dest.writeString(street);
        dest.writeString(building);
        dest.writeStringList(photos);
        dest.writeString(workingHours);
        dest.writeStringList(services);
        dest.writeStringList(amenities);
    }

    public interface AvailabilityCallback {
        void onComplete(boolean isAvailable);
    }

    public interface BookingCallback {
        void onComplete(boolean success);
    }

    public interface OnTimeSlotsLoadedListener {
        void onTimeSlotsLoaded(List<String> availableSlots);
    }
}