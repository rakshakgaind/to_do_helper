package com.project.smart_to_do.fragments.list


import android.app.*
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.nordan.dialog.NordanAlertDialog
import com.project.smart_to_do.R
import com.project.smart_to_do.adapters.ListAdapter
import com.project.smart_to_do.broadcast.channelID
import com.project.smart_to_do.broadcast.messageExtra
import com.project.smart_to_do.broadcast.titleExtra
import com.project.smart_to_do.data.Task
import com.project.smart_to_do.databinding.FragmentListBinding
import com.project.smart_to_do.utils.DateFormatUtil
import com.project.smart_to_do.viewmodel.TaskViewModel
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

@RequiresApi(Build.VERSION_CODES.O)
class ListFragment : Fragment(R.layout.fragment_list), SearchView.OnQueryTextListener {

    private lateinit var binding: FragmentListBinding
    private lateinit var mTaskViewModel: TaskViewModel
    private lateinit var adapter: ListAdapter
    private lateinit var deletedTask: Task
    private lateinit var selectedTask: Task

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentListBinding.bind(view)
        binding.toolbar.inflateMenu(R.menu.main_menu)
        initAdapter()
        initViewModel()
        menuSelection()
        swipeToHandleEvent()
        navigate()
    }


    private fun navigate() {
        binding.moveToAddBtn.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_addingFragment)
        }
    }

    private fun initViewModel() {
        mTaskViewModel = ViewModelProvider(this)[TaskViewModel::class.java]
        mTaskViewModel.readNotDoneData().observe(viewLifecycleOwner) { tasks ->
            adapter.setData(tasks)

            if (tasks.size == 0) {
                binding.emptyDesTxt.visibility = View.VISIBLE
                binding.emptyAnim.visibility = View.VISIBLE
                binding.emptyLogo.visibility = View.GONE
            } else {
                binding.emptyLogo.visibility = View.VISIBLE
                binding.emptyDesTxt.visibility = View.GONE
                binding.emptyAnim.visibility = View.GONE
            }
            for (task in tasks) {
                if (!task.isDone && DateFormatUtil.hourly()
                        .parse(task.date)!!.time > System.currentTimeMillis()
                )
                    scheduleNotification(task)
            }

        }
    }

    private fun initAdapter() {
        adapter = ListAdapter { task ->
            handlerTaskData(task)
        }

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    binding.moveToAddBtn.startAnimation(
                        AnimationUtils.loadAnimation(
                            binding.moveToAddBtn.context,
                            com.google.android.material.R.anim.abc_fade_in
                        )
                    )
                    binding.moveToAddBtn.visibility = View.VISIBLE
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0 && binding.moveToAddBtn.visibility == View.VISIBLE) {
                    binding.moveToAddBtn.visibility = View.GONE
                }
            }
        }
        binding.recyclerView.addOnScrollListener(scrollListener)


    }



    private fun handlerTaskData(task: Task) {
        mTaskViewModel.updateTask(task)
        cancelNotification(task)
    }


    private fun swipeToHandleEvent() {
        ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        deletedTask = adapter.getTaskAt(viewHolder.layoutPosition)
                        mTaskViewModel.deleteTask(deletedTask)
                        Snackbar.make(
                            binding.recyclerView,
                            "${deletedTask.title} has just been deleted!",
                            Snackbar.LENGTH_LONG
                        )
                            .setAction("Undo") {
                                mTaskViewModel.addTask(deletedTask)
                            }.show()
                    }
                    ItemTouchHelper.RIGHT -> {
                        selectedTask = adapter.getTaskAt(viewHolder.layoutPosition)
                        if (!selectedTask.isDone) {
                            selectedTask.isDone = true
                            mTaskViewModel.updateTask(selectedTask)
                            adapter.notifyItemChanged(viewHolder.layoutPosition)
                            Snackbar.make(
                                binding.recyclerView,
                                "You have just done ${selectedTask.title} task!",
                                Snackbar.LENGTH_LONG
                            )
                                .setAction("Undo") {
                                    selectedTask.isDone = false
                                    mTaskViewModel.updateTask(selectedTask)
                                }.show()
                        } else {
                            Toast.makeText(
                                context,
                                "You have already done this task!",
                                Toast.LENGTH_LONG
                            ).show()
                            mTaskViewModel.updateTask(selectedTask)
                        }
                    }
                }

            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                    .addCornerRadius(1, 10)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(context!!, R.color.red))
                    .addSwipeLeftActionIcon(R.drawable.ic_baseline_delete_24)
                    .addSwipeRightBackgroundColor(
                        ContextCompat.getColor(
                            context!!,
                            R.color.teal_200
                        )
                    )
                    .addSwipeRightActionIcon(R.drawable.ic_baseline_done_24)
                    .create()
                    .decorate()


                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }

        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun menuSelection() {
        binding.toolbar.setOnMenuItemClickListener { menu ->
            when (menu.itemId) {
                R.id.menu_search -> {
                    val searchView = menu.actionView as? SearchView
                    searchView?.isSubmitButtonEnabled = true
                    searchView?.setOnQueryTextListener(this)
                    true
                }
                R.id.menu_delete -> {
                    NordanAlertDialog.Builder(context as Activity?)
                        .setAnimation(com.nordan.dialog.Animation.POP)
                        .isCancellable(true)
                        .setTitle("WARNING YOU ARE ABOUT TO DELETE ALL TASKS!")
                        .setMessage("You won't be able to recover these tasks!")
                        .setIcon(R.drawable.warning, false)
                        .setPositiveBtnText("Sure!")
                        .setNegativeBtnText("Nah!")
                        .onPositiveClicked {
                            mTaskViewModel.deleteAllNotDoneTask()
                            Toast.makeText(
                                context,
                                "Deleted all task successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        .build().show()
                    true
                }
                R.id.menu_sortByDate -> {
                    mTaskViewModel.readNotDoneData().observe(viewLifecycleOwner) { task ->
                        task.sortBy { DateFormatUtil.hourly().parse(it.date)!!.time }
                        adapter.setData(task)
                    }
                    true
                }
                R.id.menu_sortByTitle -> {
                    mTaskViewModel.readNotDoneData().observe(viewLifecycleOwner) { task ->
                        task.sortBy { it.title }
                        adapter.setData(task)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            searchDB(query)
        }
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (query != null) {
            searchDB(query)
        }
        return true
    }

    private fun searchDB(query: String) {
        val searchQuery = "%$query%"

        mTaskViewModel.searchDbByTitle(searchQuery).observe(this) { list ->
            list.let {
                adapter.setData(it)
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Notification Channel for superbToDo"
        val desc = "A Description of the Channel"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelID, name, importance)
        channel.description = desc
        val notificationManager =
            activity?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun scheduleNotification(task : Task) {
        val intent = Intent(context, Notification::class.java)
        intent.putExtra(titleExtra, task.title)
        intent.putExtra("notificationID", id)
        intent.putExtra(messageExtra, "${task.description}\n in ${task.date}")
        val setTime = DateFormatUtil.hourly().parse(task.date)!!.time
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = activity?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, setTime, pendingIntent)
    }

    private fun cancelNotification(task : Task) {
        val intent = Intent(context, Notification::class.java)
        intent.putExtra(titleExtra, task.title)
        intent.putExtra("notificationID", id)
        intent.putExtra(messageExtra, "${task.description}\n in ${task.date}")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = activity?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }



}



