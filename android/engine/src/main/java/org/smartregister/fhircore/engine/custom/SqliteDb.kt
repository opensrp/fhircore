package org.smartregister.fhircore.engine.custom

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import timber.log.Timber

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 08-03-2023.
 */
class SqliteDb   // creating a constructor for our database handler.
    (context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    // below method is for creating a database by running a sqlite query
    override fun onCreate(db: SQLiteDatabase) {
        // on below line we are creating
        // an sqlite query and we are
        // setting our column names
        // along with their data types.
        /*val query = ("CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + NAME_COL + " TEXT,"
                + DURATION_COL + " TEXT,"
                + DESCRIPTION_COL + " TEXT,"
                + TRACKS_COL + " TEXT)")

        // at last we are calling a exec sql
        // method to execute above sql query
        db.execSQL(query)*/
    }

    // this method is use to add new course to our sqlite database.
    fun addNewCourse(
        courseName: String?,
        courseDuration: String?,
        courseDescription: String?,
        courseTracks: String?
    ) {

        // on below line we are creating a variable for
        // our sqlite database and calling writable method
        // as we are writing data in our database.
        val db = this.writableDatabase

        // on below line we are creating a
        // variable for content values.
        val values = ContentValues()

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(NAME_COL, courseName)
        values.put(DURATION_COL, courseDuration)
        values.put(DESCRIPTION_COL, courseDescription)
        values.put(TRACKS_COL, courseTracks)

        // after adding all values we are passing
        // content values to our table.
        db.insert(TABLE_NAME, null, values)

        // at last we are closing our
        // database after adding database.
        db.close()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // this method is called to check if the table exists already.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME)
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase?) {
        super.onOpen(db)

        Timber.e("DB $db openned")
    }

    companion object {
        // creating a constant variables for our database.
        // below variable is for our database name.
        private const val DB_NAME = "resources.db"

        // below int is our database version
        private const val DB_VERSION = 1

        // below variable is for our table name.
        private const val TABLE_NAME = "mycourses"

        // below variable is for our id column.
        private const val ID_COL = "id"

        // below variable is for our course name column
        private const val NAME_COL = "name"

        // below variable id for our course duration column.
        private const val DURATION_COL = "duration"

        // below variable for our course description column.
        private const val DESCRIPTION_COL = "description"

        // below variable is for our course tracks column.
        private const val TRACKS_COL = "tracks"
    }
}
