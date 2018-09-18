package mil.health.sdd.nearbyclient1;

import android.content.SharedPreferences;

import java.util.UUID;

public class GeneralPreferencesHelper {
    private SharedPreferences preferences;

    public GeneralPreferencesHelper(SharedPreferences preferences){
        this.preferences = preferences;
    }

    public String getUUID(String stringName){
        String uuid = preferences.getString(stringName,"");

        if(uuid.length() == 0){
            uuid = UUID.randomUUID().toString();
            preferences.edit().putString(stringName,uuid);
        }
        return uuid;
    }

    public SharedPreferences getPreferences() {
        return preferences;
    }
}
