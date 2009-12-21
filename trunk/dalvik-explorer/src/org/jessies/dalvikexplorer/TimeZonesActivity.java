package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import android.view.*;
import java.util.*;

public class TimeZonesActivity extends ListActivity {
    private static class TimeZoneListItem {
        private final TimeZone timeZone;
        private TimeZoneListItem(TimeZone timeZone) {
            this.timeZone = timeZone;
        }
        @Override public String toString() {
            String result = timeZone.getID();
            if (timeZone.equals(TimeZone.getDefault())) {
                result += " (default)";
            }
            return result;
        }
    }
    private static final List<TimeZoneListItem> TIME_ZONES = gatherTimeZones();
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(new ArrayAdapter<TimeZoneListItem>(this, android.R.layout.simple_list_item_1, TIME_ZONES));
        setTitle("Time Zones (" + TIME_ZONES.size() + ")");
    }
    
    @Override protected void onListItemClick(ListView l, View v, int position, long id) {
        final Intent intent = new Intent(this, TimeZoneActivity.class);
        intent.putExtra("org.jessies.dalvikexplorer.TimeZone", TIME_ZONES.get(position).timeZone.getID());
        startActivity(intent);
    }
    
    private static List<TimeZoneListItem> gatherTimeZones() {
        final String[] availableIds = TimeZone.getAvailableIDs();
        final TimeZone defaultTimeZone = TimeZone.getDefault();
        // Put the default time zone at the top of the list...
        final List<TimeZoneListItem> result = new ArrayList<TimeZoneListItem>(availableIds.length);
        result.add(new TimeZoneListItem(defaultTimeZone));
        // ...followed by all the others.
        for (String id : availableIds) {
            final TimeZone timeZone = TimeZone.getTimeZone(id);
            if (!timeZone.equals(defaultTimeZone)) {
                result.add(new TimeZoneListItem(timeZone));
            }
        }
        return result;
    }
}