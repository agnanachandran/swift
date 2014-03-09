package ca.pluszero.emotive.fragments;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubeIntents;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ca.pluszero.emotive.MusicLauncher;
import ca.pluszero.emotive.NetworkManager;
import ca.pluszero.emotive.R;
import ca.pluszero.emotive.YouTubeClient;
import ca.pluszero.emotive.adapters.PlacesAutoCompleteAdapter;
import ca.pluszero.emotive.adapters.YouTubeListAdapter;
import ca.pluszero.emotive.models.Option;
import ca.pluszero.emotive.models.YouTubeVideo;

public class MainSectionFragment extends Fragment implements View.OnClickListener {
    /**
     * The fragment argument representing the section number for this fragment.
     */
    public static final String ARG_SECTION_NUMBER = "section_number";

    private Button[] primaryButtons;

    private Button bFirstButton;
    private Button bSecondButton;
    private Button bThirdButton;
    private Button bFourthButton;
    private Button bFifthButton;
    private Button bSixthButton;

    private ListView lvQueryResults;

    private SimpleCursorAdapter mAdapter;

    private int mPrimaryOption = 0;

    private AutoCompleteTextView etSearchView;
    private View rootView;
    private TextView mainTextView;

    public MainSectionFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_main, container, false);

        lvQueryResults = (ListView) rootView.findViewById(R.id.lvQueryResults);
        mainTextView = (TextView) rootView.findViewById(R.id.mainTextview);

        // tvSearchQuery.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
        setupPrimaryButtons(rootView);

        etSearchView = (AutoCompleteTextView) rootView.findViewById(R.id.mainSearchView);
        etSearchView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(v.getText());
                    return true;
                }
                return false;
            }

        });

        setHasOptionsMenu(true);
        return rootView;
    }

    private void setupPrimaryButtons(View rootView) {
        // Set up 3 primary search buttons
        bFirstButton = (Button) rootView.findViewById(R.id.bFirstOption);
        bSecondButton = (Button) rootView.findViewById(R.id.bSecondOption);
        bThirdButton = (Button) rootView.findViewById(R.id.bThirdOption);
        bFourthButton = (Button) rootView.findViewById(R.id.bFirstOption);
        bFifthButton = (Button) rootView.findViewById(R.id.bSecondOption);
        bSixthButton = (Button) rootView.findViewById(R.id.bThirdOption);

        primaryButtons = new Button[]{bFirstButton, bSecondButton, bThirdButton, bFourthButton, bFifthButton, bSixthButton};
        for (Button b : primaryButtons) {
            b.setOnClickListener(this);
        }
    }

    private void placeSearchOptions() {
//        setupSecondaryOptions(getResources().getStringArray(R.array.search_options), R.string.search_options_title_label);
        etSearchView.setOnItemClickListener(null); // TODO: do this for other options
    }

    private void placeFindMeOptions() {
//        setupSecondaryOptions(getResources().getStringArray(R.array.find_me_options), R.string.find_options_title_label);
        ((AutoCompleteTextView) etSearchView).setAdapter(new PlacesAutoCompleteAdapter(
                getActivity(), R.layout.simple_list_item));
        ((AutoCompleteTextView) etSearchView).setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                etSearchView.setText((String) adapterView.getItemAtPosition(position));
            }
        });
    }

    private void performSearch(CharSequence query) {
        if (mPrimaryOption == Option.Verb.SEARCH.val) {
            startGoogleSearchAnything(query);
        } else if (mPrimaryOption == Option.Verb.LISTEN_TO.val) {
            startMusicSearchDevice(query.toString());
        } else if (mPrimaryOption == Option.Verb.FIND_ME.val) {
            startMapsSearch(query);
        }
    }

    private void startYouTubeSearch(CharSequence query) {
        if (NetworkManager.isConnected(getActivity())) {
            YouTubeClient.getYouTubeSearch(query.toString(), new JsonHttpResponseHandler() {
                @Override
                public void onStart() {
                }

                @Override
                public void onSuccess(JSONObject response) {
                    final List<YouTubeVideo> videos = new ArrayList<YouTubeVideo>();
                    try {

                        final JSONArray searchJsonItems = response.getJSONArray("items");
                        for (int i = 0; i < searchJsonItems.length(); i++) {
                            final JSONObject videoObject = searchJsonItems.getJSONObject(i);
                            final String videoId = videoObject.getJSONObject("id").getString(
                                    "videoId");
                            // Get statistics; specifically # of views
                            YouTubeClient.getYouTubeVideo(videoId, new JsonHttpResponseHandler() {
                                @Override
                                public void onStart() {
                                }

                                @Override
                                public synchronized void onSuccess(JSONObject response) {
                                    try {
                                        JSONArray jsonItems = response.getJSONArray("items");
                                        JSONObject item = jsonItems.getJSONObject(0);
                                        int viewCount = Integer.parseInt(item.getJSONObject(
                                                "statistics").getString("viewCount"));
                                        JSONObject snippet = videoObject.getJSONObject("snippet");
                                        String videoName = snippet.getString("title");
                                        String channelName = snippet.getString("channelTitle");
                                        // get # of views
                                        String thumbnailUrl = snippet.getJSONObject("thumbnails")
                                                .getJSONObject("medium").getString("url");
                                        videos.add(new YouTubeVideo(videoId, videoName,
                                                thumbnailUrl, viewCount, channelName));
                                        if (videoObject.equals(searchJsonItems.getJSONObject(searchJsonItems.length() - 1))) {

                                            bringUpListviewAndDismissKeyboard();
                                            lvQueryResults.setVisibility(View.VISIBLE);
                                            lvQueryResults.setAdapter(new YouTubeListAdapter(
                                                    getActivity(), videos));
                                            lvQueryResults
                                                    .setOnItemClickListener(new OnItemClickListener() {
                                                        @Override
                                                        public void onItemClick(
                                                                AdapterView<?> parent, View view,
                                                                int position, long id) {
                                                            String videoId = ((YouTubeVideo) lvQueryResults
                                                                    .getItemAtPosition(position))
                                                                    .getId();
                                                            Intent intent = YouTubeIntents
                                                                    .createPlayVideoIntent(
                                                                            getActivity(), videoId);
                                                            startActivity(intent);
                                                        }
                                                    });
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });

                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                    }
                }
            });
        } else {
            // No interwebs; display Toast. TODO
        }
        // Intent intent = new Intent(Intent.ACTION_SEARCH);
        // intent.setPackage("com.google.android.youtube");
        // intent.putExtra("search_query", query);
        // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // startActivity(intent);
    }

    private void startMapsSearch(CharSequence query) {
        String url = "geo:0,0?q=" + query;
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }

    // launches an activity that searches Google for the user's query in a
    // browser
    private void startGoogleSearchAnything(CharSequence query) {
        Intent browserIntent = new Intent(Intent.ACTION_WEB_SEARCH);
        browserIntent.putExtra(SearchManager.QUERY, query.toString());
        startActivity(browserIntent);
    }

    private void startMusicSearchDevice(String query) {
        MusicLauncher musicLauncher = MusicLauncher.getInstance(this);
        musicLauncher.searchMusic(query);
        String[] artistColumns = {MediaStore.Audio.Media.ARTIST};
        int[] mSongListItems = {R.id.tvQueryTitleCard};
        mAdapter = new SimpleCursorAdapter(getActivity(), R.layout.row_card, null, artistColumns,
                mSongListItems);
        lvQueryResults.setVisibility(View.VISIBLE);
        lvQueryResults.setAdapter(mAdapter);
        lvQueryResults.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor c = (Cursor) mAdapter.getItem(position);
                int index = c.getColumnIndex(MediaStore.Audio.Media.DATA);
                String songPath = c.getString(index);
                Log.d(getTag(), songPath);
                File file = new File(songPath);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), "audio/*");
                startActivity(intent);
            }
        });
    }

    private void bringUpListviewAndDismissKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etSearchView.getWindowToken(), 0);

        // TODO: Show listview
    }

    @Override
    public void onClick(View v) {
        Button b = (Button) v;
        switch (b.getId()) {
            case R.id.bFirstOption:
                mPrimaryOption = 1;
                break;
            case R.id.bSecondOption:
                mPrimaryOption = 2;
                break;
            case R.id.bThirdOption:
                mPrimaryOption = 3;
                break;
            case R.id.bFourthOption:
                mPrimaryOption = 4;
                break;
            case R.id.bFifthOption:
                mPrimaryOption = 5;
                break;
            case R.id.bSixthOption:
                mPrimaryOption = 6;
                break;
        }
        setupButton(b.getText().toString());

    }

    private void setupButton(String btnText) {
        rootView.findViewById(R.id.horizontalButtonLinearLayout1).setVisibility(View.GONE);
        rootView.findViewById(R.id.horizontalButtonLinearLayout2).setVisibility(View.GONE);
        mainTextView.setText(btnText);

    }

    public SimpleCursorAdapter getAdapter() {
        return mAdapter;
    }
}
