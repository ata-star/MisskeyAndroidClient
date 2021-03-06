package jp.panta.misskeyandroidclient.view.notes.detail

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.model.Page
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModelFactory
import jp.panta.misskeyandroidclient.viewmodel.notes.detail.NoteDetailViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.detail.NoteDetailViewModelFactory
import jp.panta.misskeyandroidclient.viewmodel.setting.page.PageableTemplate
import kotlinx.android.synthetic.main.fragment_note_detail.*
import java.lang.IllegalArgumentException

class NoteDetailFragment : Fragment(R.layout.fragment_note_detail){

    companion object{
        private const val EXTRA_NOTE_ID = "jp.panta.misskeyandroidclinet.view.notes.detail.EXTRA_NOTE_ID"
        private const val EXTRA_SHOW = "jp.panta.misskeyandroidclinet.view.notes.detail.EXTRA_SHOW"
        fun newInstance(noteId: String): NoteDetailFragment{
            return NoteDetailFragment().apply{
                arguments = Bundle().apply{
                    putString(EXTRA_NOTE_ID, noteId)
                }
            }
        }

        fun newInstance(show: Page.Show): NoteDetailFragment{
            return NoteDetailFragment().apply{
                arguments = Bundle().apply{
                    putSerializable(EXTRA_SHOW, show)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val show = arguments?.getSerializable(EXTRA_SHOW) as? Page.Show
            ?: Page.Show(arguments?.getString(EXTRA_NOTE_ID)!!)

        val miApplication = context?.applicationContext as MiApplication
        miApplication.currentAccount.observe(viewLifecycleOwner, Observer {ar ->
            val notesViewModel = ViewModelProvider(requireActivity(), NotesViewModelFactory(ar, miApplication))[NotesViewModel::class.java]
            val noteDetailViewModel = ViewModelProvider(this, NoteDetailViewModelFactory(ar, miApplication, show))[NoteDetailViewModel::class.java]

            noteDetailViewModel.loadDetail()
            val adapter = NoteDetailAdapter(
                noteDetailViewModel = noteDetailViewModel,
                notesViewModel = notesViewModel,
                viewLifecycleOwner = viewLifecycleOwner
            )
            noteDetailViewModel.notes.observe(viewLifecycleOwner, Observer {
                adapter.submitList(it)
            })

            noteDetailViewModel.loadDetail()

            notes_view.adapter = adapter
            notes_view.layoutManager = LinearLayoutManager(context)
        })
    }
}