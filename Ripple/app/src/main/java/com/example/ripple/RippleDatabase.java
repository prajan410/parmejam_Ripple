package com.example.ripple;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SosPacket.class}, version = 3, exportSchema = false)
public abstract class RippleDatabase extends RoomDatabase {
    public abstract SosPacketDao sosPacketDao();

    private static volatile RippleDatabase INSTANCE;
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE packets ADD COLUMN uploaded INTEGER NOT NULL DEFAULT 0");
        }
    };
    public static RippleDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (RippleDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            RippleDatabase.class,
                            "ripple_database"
                    ).fallbackToDestructiveMigration().build();
                }
            }
        }
        return INSTANCE;
    }
}
