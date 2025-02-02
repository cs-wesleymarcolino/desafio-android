package com.felipe.palma.desafioconcrete.ui.repository;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.felipe.palma.desafioconcrete.R;
import com.felipe.palma.desafioconcrete.domain.model.Item;
import com.felipe.palma.desafioconcrete.ui.adapter.AnimationItem;
import com.felipe.palma.desafioconcrete.ui.adapter.InfiniteScrollListener;
import com.felipe.palma.desafioconcrete.ui.adapter.RecyclerItemClickListener;
import com.felipe.palma.desafioconcrete.ui.adapter.RepositoryAdapter;
import com.felipe.palma.desafioconcrete.ui.adapter.decoration.ItemOffsetDecoration;
import com.felipe.palma.desafioconcrete.ui.pullrequest.PullRequestActivity;
import com.felipe.palma.desafioconcrete.utils.Config;
import com.felipe.palma.desafioconcrete.utils.UnsplashApplication;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;


/**
 * Created by Felipe Palma on 11/07/2019.
 */

public class RepositoriesActivity extends AppCompatActivity implements RepositoriesContract.View, SwipeRefreshLayout.OnRefreshListener  {

    private RepositoriesContract.Presenter mPresenter;
    private RepositoryAdapter mRepoAdapter;
    private int mPage = 1;
    private ProgressDialog dialog;
    private Context context = this;

    private ArrayList<Item> mRepoList = new ArrayList<>();
    private InfiniteScrollListener infiniteScrollListener;
    private LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(this);
    private Parcelable listState;

    //Views

    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;

    @BindView(R.id.toobar) Toolbar toolbar;
    @BindView(R.id.refresh) SwipeRefreshLayout mSwipeRefreshLayout;

    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        INSTANCIA BUTTERKNIFE
         */
        ButterKnife.bind(this);

        mPresenter = new RepositoriesPresenter(this);

        mSwipeRefreshLayout.setOnRefreshListener(this);
        mSwipeRefreshLayout.setEnabled( true );

        setupToolBar();
        setupAdapter();

        setupRecyclerView();

        if(!UnsplashApplication.hasNetwork() ){
            Toast.makeText(context, getResources().getString(R.string.internet_offline),Toast.LENGTH_LONG).show();
            //finish();
        }


        if (savedInstanceState != null){
            mRepoList = savedInstanceState.getParcelableArrayList(Config.SAVE_LIST_STATE);
            listState = savedInstanceState.getParcelable(Config.SAVE_STATE);
            mPage = savedInstanceState.getInt(Config.SAVE_STATE_PAGE);
            displayData();

            restoreLayoutManagerPosition();
            mRepoAdapter.notifyDataSetChanged();
        }else {
            setupPresenter();
        }


        setupEndLess();

    }



    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        listState = mLinearLayoutManager.onSaveInstanceState();
        state.putParcelableArrayList(Config.SAVE_LIST_STATE, mRepoList);
        state.putParcelable(Config.SAVE_STATE, mRecyclerView.getLayoutManager().onSaveInstanceState());
        state.putInt(Config.SAVE_STATE_PAGE, mPage);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        mRepoList = state.getParcelableArrayList(Config.SAVE_LIST_STATE);
        listState = state.getParcelable(Config.SAVE_STATE);
        mPage = state.getInt(Config.SAVE_STATE_PAGE);
        super.onRestoreInstanceState(state);

    }


    @Override
    public void hideDialog() {
        dialog.dismiss();
    }

    @Override
    public void showDialog() {
        if (!mSwipeRefreshLayout.isRefreshing()){
            dialog = ProgressDialog.show(this, getResources().getString(R.string.load_data),
                    getResources().getString(R.string.load_wait), false);

        }

    }


    @Override
    public void showError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void showRepositories(ArrayList<Item> items) {
        //setupAdapter();
        mRepoAdapter.addRepoItems(items);

        mRepoList = mRepoAdapter.getItems();
        showAnimation();

        if (mSwipeRefreshLayout.isRefreshing()){
            mSwipeRefreshLayout.setRefreshing(false);
        }

    }

    @Override
    public void showAnimation() {
        Context context = mRecyclerView.getContext();
        AnimationItem mAnimationItem =  new AnimationItem("Slide from bottom", R.anim.layout_animation_from_bottom);
        final LayoutAnimationController controller =
                AnimationUtils.loadLayoutAnimation(context, mAnimationItem.getResourceId());

        mRecyclerView.setLayoutAnimation(controller);
        mRecyclerView.getAdapter().notifyDataSetChanged();
        mRecyclerView.scheduleLayoutAnimation();
    }


    private void setupToolBar(){
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Github JavaPop");
    }

    private void setupPresenter() {
        mPresenter = new RepositoriesPresenter(this);
        mPresenter.loadRepositories(mPage);

    }

    private void setupAdapter(){
        mRepoAdapter = new RepositoryAdapter(this, mRepoList, recyclerItemClickListener);
        mRecyclerView.setAdapter(mRepoAdapter);
    }
    private void displayData(){
        //setupEndLess();
        setupAdapter();

    }

    private void setupRecyclerView() {
        int spacing = getResources().getDimensionPixelOffset(R.dimen.default_spacing_small);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new ItemOffsetDecoration(spacing));
    }

    private void setupEndLess(){
        infiniteScrollListener = new InfiniteScrollListener(mLinearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {

                mPresenter.loadRepositories(page);


            }
        };

        mRecyclerView.addOnScrollListener(infiniteScrollListener);
    }

    private void restoreLayoutManagerPosition() {
        if (listState != null) {
            mRecyclerView.getLayoutManager().onRestoreInstanceState(listState);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_list, menu);


        setupSearchView(menu);


        return true;
    }


    private void setupSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();
        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mRepoAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                mRepoAdapter.getFilter().filter(query);
                return false;
            }
        });
    }

    /**
     * RecyclerItem click event listener
     * */
    private RecyclerItemClickListener<Item> recyclerItemClickListener = item -> {
        String owner = item.getOwner().getLogin();
        String repository = item.getName();

        if(UnsplashApplication.hasNetwork()){
            Intent mIntent = new Intent(this, PullRequestActivity.class);
            mIntent.putExtra(Config.OWNER,owner);
            mIntent.putExtra(Config.REPO,repository);
            startActivity(mIntent);
        }else{
            Toast.makeText(context, getResources().getString(R.string.internet_offline),Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onRefresh() {
        mRepoAdapter.clearItems();
        mPresenter.loadRepositories(mPage);
        displayData();
        mRepoAdapter.notifyDataSetChanged();

    }




}
