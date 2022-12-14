package com.example.superbtodo.data
// TaskDao was built for the purpose containing the methods used for accessing the database
// TaskDao ( data access object ) is a interface cuz it provides methods that the rest of the app uses to interact with data in the table
import androidx.lifecycle.LiveData
import androidx.room.*
import com.project.smart_to_do.data.Task

@Dao
interface TaskDao {
    // means it will be just ignore if there is a new exactly same task then we're gonna just ignore it
    @Insert
    suspend fun addTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)


    @Query("SELECT * FROM task_table ORDER BY date ASC")
    fun readAllData(): LiveData<MutableList<Task>>

    @Query("SELECT * FROM task_table  WHERE isDone=0 ORDER BY date ASC " )
    fun readNotDoneData(): LiveData<MutableList<Task>>

    @Query("SELECT * FROM task_table WHERE isDone=1 ORDER BY date ASC" )
    fun readDoneData(): LiveData<MutableList<Task>>

    @Query("DELETE FROM task_table WHERE isDone =0")
    fun deleteAllNotDoneTasks()

    @Query("DELETE FROM task_table WHERE isDone =1")
    fun deleteAllDoneTasks()

    @Query("SELECT * FROM task_table WHERE title LIKE :searchQuery AND isDone=0")
    fun searchDbByTitle(searchQuery: String) : LiveData<MutableList<Task>>

    @Query("SELECT * FROM task_table WHERE title LIKE :searchQuery AND isDone=1")
    fun searchIsDoneDbByTitle(searchQuery: String) : LiveData<MutableList<Task>>

    @Query("SELECT * FROM task_table WHERE date LIKE :searchQuery AND isDone=0")
    fun calendarSearch(searchQuery: String) : LiveData<MutableList<Task>>

}
