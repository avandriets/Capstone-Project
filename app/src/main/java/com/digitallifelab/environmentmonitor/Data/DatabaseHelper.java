package com.digitallifelab.environmentmonitor.Data;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.digitallifelab.environmentmonitor.R;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "environmentMonitor.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 36;

    // the DAO object we use to access the SimpleData table
    private Dao<AccountsStore, String> AccountsDao = null;
    private RuntimeExceptionDao<AccountsStore, String> AccountRuntimeDao = null;

    private Dao<PointsStore, Long> PointsDao = null;
    private RuntimeExceptionDao<PointsStore, Long> PointsRuntimeDao = null;

    private Dao<PicturesStore, Long> PicturesDao = null;
    private RuntimeExceptionDao<PicturesStore, Long> PicturesRuntimeDao = null;

    private Dao<MessagesStore, Long> MessagesDao = null;
    private RuntimeExceptionDao<MessagesStore, Long> MessagesRuntimeDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {

        try {
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, AccountsStore.class);
            TableUtils.createTable(connectionSource, PointsStore.class);
            TableUtils.createTable(connectionSource, PicturesStore.class);
            TableUtils.createTable(connectionSource, MessagesStore.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onUpgrade");
            TableUtils.dropTable(connectionSource, AccountsStore.class, true);
            TableUtils.dropTable(connectionSource, PointsStore.class, true);
            TableUtils.dropTable(connectionSource, PicturesStore.class, true);
            TableUtils.dropTable(connectionSource, MessagesStore.class, true);

            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<AccountsStore, String> getAccountDao() throws SQLException {
        if (AccountsDao == null) {
            AccountsDao = getDao(AccountsStore.class);
        }
        return AccountsDao;
    }

    /**
     * Returns the RuntimeExceptionDao (Database Access Object) version of a Dao for our SimpleData class. It will
     * create it or just give the cached value. RuntimeExceptionDao only through RuntimeExceptions.
     */
    public RuntimeExceptionDao<AccountsStore, String> getAccountsDataDao() {
        if (AccountRuntimeDao == null) {
            AccountRuntimeDao = getRuntimeExceptionDao(AccountsStore.class);
        }
        return AccountRuntimeDao;
    }

    //Points
    public Dao<PointsStore, Long> getPointsDao() throws SQLException {
        if (PointsDao == null) {
            PointsDao = getDao(PointsStore.class);
        }
        return PointsDao;
    }
    
    public RuntimeExceptionDao<PointsStore, Long> getPointsDataDao() {
        if (PointsRuntimeDao == null) {
            PointsRuntimeDao = getRuntimeExceptionDao(PointsStore.class);
        }
        return PointsRuntimeDao;
    }

    //pictures
    public Dao<PicturesStore, Long> getPicturesDao() throws SQLException {
        if (PicturesDao == null) {
            PicturesDao = getDao(PicturesStore.class);
        }
        return PicturesDao;
    }

    public RuntimeExceptionDao<PicturesStore, Long> getPicturesDataDao() {
        if (PicturesRuntimeDao == null) {
            PicturesRuntimeDao = getRuntimeExceptionDao(PicturesStore.class);
        }
        return PicturesRuntimeDao;
    }

    //Messages
    public Dao<MessagesStore, Long> getMessagesDao() throws SQLException {
        if (MessagesDao == null) {
            MessagesDao = getDao(MessagesStore.class);
        }
        return MessagesDao;
    }

    public RuntimeExceptionDao<MessagesStore, Long> getMessagesDataDao() {
        if (MessagesRuntimeDao == null) {
            MessagesRuntimeDao = getRuntimeExceptionDao(MessagesStore.class);
        }
        return MessagesRuntimeDao;
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        AccountsDao = null;
        AccountRuntimeDao = null;
    }
}
