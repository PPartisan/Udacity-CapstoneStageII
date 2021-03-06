package com.github.ppartisan.fishlesscycle.ui;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.ppartisan.fishlesscycle.R;
import com.github.ppartisan.fishlesscycle.adapter.TankCardCallbacks;
import com.github.ppartisan.fishlesscycle.adapter.TanksAdapter;
import com.github.ppartisan.fishlesscycle.data.Contract;
import com.github.ppartisan.fishlesscycle.model.Tank;
import com.github.ppartisan.fishlesscycle.setup.SetUpWizardActivity;
import com.github.ppartisan.fishlesscycle.strategy.Strategy;
import com.github.ppartisan.fishlesscycle.util.AppUtils;
import com.github.ppartisan.fishlesscycle.util.ConversionUtils.DosageUnit;
import com.github.ppartisan.fishlesscycle.util.PreferenceUtils;
import com.github.ppartisan.fishlesscycle.util.PreferenceUtils.VolumeUnit;
import com.github.ppartisan.fishlesscycle.util.TankUtils;
import com.github.ppartisan.fishlesscycle.util.ViewUtils;
import com.github.ppartisan.fishlesscycle.view.EmptyRecyclerView;

import java.util.List;

public final class MainFragment extends Fragment implements View.OnClickListener, Toolbar.OnMenuItemClickListener, TankCardCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_RETRIEVAL = 2;

    private ImageCapture mImageCapture = new ImageCapture();

    private TanksAdapter mAdapter;

    private Toolbar mToolbar;

    public static MainFragment newInstance(String uninstallMessage) {

        Bundle args = new Bundle();
        args.putString(MainActivity.UNINSTALL_MESSAGE_EXTRA, uninstallMessage);

        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        setHasOptionsMenu(true);

        mToolbar = (Toolbar) view.findViewById(R.id.fm_toolbar);
        mToolbar.inflateMenu(R.menu.main_menu);
        mToolbar.setOnMenuItemClickListener(this);

        final FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fm_fab);
        fab.setOnClickListener(this);

        final ImageView emptyImageView = (ImageView) view.findViewById(R.id.fm_empty_view_image);
        Glide.with(getContext()).load(R.drawable.f_cycle_grey).into(emptyImageView);

        final EmptyRecyclerView recycler = (EmptyRecyclerView) view.findViewById(R.id.fm_recycler);
        recycler.setEmptyView(view.findViewById(R.id.fm_empty_view));
        recycler.setCallback(new EmptyRecyclerView.Callback() {
            @Override
            public void onEmpty() {
                if(!fab.isShown()) fab.show();
            }
        });

        final @DosageUnit int dosUnitType =
                PreferenceUtils.getDosageUnitType(getContext());

        final @VolumeUnit int volUnitType =
                PreferenceUtils.getVolumeUnit(getContext());

        final int colCount = getResources().getInteger(R.integer.fm_recycler_column_count);
        mAdapter = new TanksAdapter(this, null, dosUnitType, volUnitType);
        recycler.setAdapter(mAdapter);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), colCount));
        recycler.setItemAnimator(new DefaultItemAnimator());

        if (savedInstanceState != null) {
            mImageCapture = savedInstanceState.getParcelable(ImageCapture.KEY);
        }

        return view;

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final String uninstallMessage = getMessage();
        if(uninstallMessage != null) {
            buildUninstallMessageSnackBar(uninstallMessage).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ImageCapture.KEY, mImageCapture);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PreferenceManager.getDefaultSharedPreferences(getContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void updateTankList(List<Tank> tanks) {
        mAdapter.updateTanksList(tanks);
    }

    private String getMessage() {
        return getArguments().getString(MainActivity.UNINSTALL_MESSAGE_EXTRA);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(getContext(), SetUpWizardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        final ActivityOptionsCompat reveal =
                ViewUtils.buildCircleRevealActivityTransition(view, getView());
        ActivityCompat.startActivity(getContext(), intent, reveal.toBundle());
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.mm_action_settings:
                Intent settingsIntent = new Intent(getContext(), SettingsActivity.class);
                settingsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                final View overflow = mToolbar.getChildAt(mToolbar.getChildCount()-1);
                final ActivityOptionsCompat reveal =
                        ViewUtils.buildCircleRevealActivityTransition(overflow, getView());
                ActivityCompat.startActivity(getContext(), settingsIntent, reveal.toBundle());
                break;
            case R.id.mm_action_about:
                final AboutDialogFragment f = AboutDialogFragment.newInstance();
                f.show(getFragmentManager(), null);
        }

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onCardClick(TanksAdapter.ViewHolder vh, int position) {

        final Tank tank = mAdapter.getTank(position);
        Intent intent = new Intent(getContext(), DetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(DetailFragment.KEY_IDENTIFIER, tank.identifier);
        intent.putExtra(DetailFragment.KEY_NAME, vh.title.getText());
        intent.putExtra(DetailFragment.KEY_DOSAGE, tank.getAmmoniaDosage().dosage);

        Pair<View,String> p2 = Pair.create((View)vh.title, ViewCompat.getTransitionName(vh.title));

        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(), p2
        );

        ActivityCompat.startActivity(getActivity(), intent, options.toBundle());
    }

    @Override
    public void onEditTankClick(View v, int position) {
        final Tank.Builder builder = new Tank.Builder(mAdapter.getTank(position));
        Intent intent = new Intent(getContext(), EditTankActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(EditTankActivity.TANK_BUILDER_KEY, builder);

        ActivityOptionsCompat optionsCompat =
                ViewUtils.buildCircleRevealActivityTransition(v, null);

        ActivityCompat.startActivity(getContext(), intent, optionsCompat.toBundle());
    }

    @Override
    public void onDeleteTankClick(int position) {
        final Tank tank = mAdapter.getTank(position);
        deleteTank(tank);
        buildTankDeletedSnackBar(tank).show();
    }

    @Override
    public void onChangePhotoCameraClick(int position) {
        final Intent photoIntent = AppUtils.buildTakePictureIntent(getActivity());
        if (photoIntent != null) {
            mImageCapture.path = photoIntent.getStringExtra(AppUtils.FILE_PATH_EXTRA);
            mImageCapture.adapterPosition = position;
            startActivityForResult(photoIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    public void onChangePhotoGalleryClick(int position) {
        mImageCapture.adapterPosition = position;
        final Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_IMAGE_RETRIEVAL);
    }

    public void refreshAdapter() {
        mAdapter.notifyDataSetChanged();
    }

    private void deleteTank(Tank tank) {
        final String where = Contract.TankEntry._ID + "=?";
        final String[] whereArgs =
                new String[] { String.valueOf(tank.identifier) };
        getContext().getContentResolver().delete(Contract.TankEntry.CONTENT_URI, where, whereArgs);
    }

    private Snackbar buildTankDeletedSnackBar(final Tank tank) {
        @SuppressWarnings("ConstantConditions")
        Snackbar snackbar = Snackbar.make(
                getView(),
                getString(R.string.deleted_template, tank.name),
                Snackbar.LENGTH_LONG
        );

        snackbar.setAction(getString(R.string.undo), new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContext().getContentResolver().insert(
                        Contract.TankEntry.CONTENT_URI, TankUtils.toContentValues(tank)
                );
            }
        });

        snackbar.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                //Action means "Undo" was clicked
                if (event == DISMISS_EVENT_ACTION || getContext() == null) {
                    return;
                }
                final String where = Contract.ReadingEntry.COLUMN_IDENTIFIER+"=?";
                final String[] whereArgs = new String[] { String.valueOf(tank.identifier) };
                getContext().getContentResolver().delete(
                        Contract.ReadingEntry.CONTENT_URI, where, whereArgs
                );
            }
        });

        return snackbar;
    }

    private Snackbar buildUninstallMessageSnackBar(String message) {
        final Snackbar s = Snackbar.make(getView(), message, Snackbar.LENGTH_INDEFINITE);
        s.setAction(R.string.dismiss, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                s.dismiss();
            }
        });
        s.setCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar snackbar, int event) {
                Strategy.get().setSyncNoticeAlreadyDisplayed(getContext(), true);
                getArguments().putString(MainActivity.UNINSTALL_MESSAGE_EXTRA, null);
            }
        });
        return s;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        final String doseUnitPrefsKey = getString(R.string.pref_dosage_unit_type_key);
        final String volUnitPrefsKey = getString(R.string.pref_volume_unit_type_key);

        if(doseUnitPrefsKey.equals(s)) {
            final @DosageUnit int unitType =
                    PreferenceUtils.getDosageUnitType(getContext());
            mAdapter.setDosageUnitType(unitType);
            mAdapter.notifyDataSetChanged();
        }

        if(volUnitPrefsKey.equals(s)) {
            final @VolumeUnit int unitType =
                    PreferenceUtils.getVolumeUnit(getContext());
            mAdapter.setVolumeUnit(unitType);
            mAdapter.notifyDataSetChanged();
        }


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode != Activity.RESULT_OK) return;

        final Tank tank = mAdapter.getTank(mImageCapture.adapterPosition);
        final ContentValues cv = new ContentValues(1);

        switch (requestCode) {
            case REQUEST_IMAGE_CAPTURE:
                getActivity().sendBroadcast(AppUtils.buildAddToGalleryIntent(mImageCapture.path));
                cv.put(Contract.TankEntry.COLUMN_IMAGE, mImageCapture.path);
                updateWithTankId(cv, tank);
                break;
            case REQUEST_IMAGE_RETRIEVAL:
                final Uri uri = data.getData();
                final String[] projection = new String[]{ MediaStore.Images.Media.DATA };

                Cursor c = null;
                String path = null;

                try {
                    c = getContext().getContentResolver().query(uri, projection, null, null, null);

                    if (c != null && c.moveToFirst()) {
                        path = c.getString(c.getColumnIndex(projection[0]));
                    }
                } finally {
                    if (c!= null && !c.isClosed()) c.close();
                }

                if(path == null) {
                    Log.i(getClass().getSimpleName(), "No path to image");
                    Toast.makeText(
                            getContext(), getString(R.string.e_no_image_path), Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                cv.put(Contract.TankEntry.COLUMN_IMAGE, AppUtils.withFilePrefix(path));
                updateWithTankId(cv, tank);
                break;
        }

    }

    private void updateWithTankId(ContentValues cv, Tank tank) {
        final String where = Contract.TankEntry._ID + "=?";
        final String[] whereArgs =
                new String[]{String.valueOf(tank.identifier)};
        getContext().getContentResolver().update(
                Contract.TankEntry.CONTENT_URI, cv, where, whereArgs
        );
    }


    private static final class ImageCapture implements Parcelable {

        static final String KEY = ImageCapture.class.getSimpleName() + ".KEY";

        String path;
        int adapterPosition;

        ImageCapture(){}

        ImageCapture(Parcel in) {
            path = in.readString();
            adapterPosition = in.readInt();
        }

        public static final Creator<ImageCapture> CREATOR = new Creator<ImageCapture>() {
            @Override
            public ImageCapture createFromParcel(Parcel in) {
                return new ImageCapture(in);
            }

            @Override
            public ImageCapture[] newArray(int size) {
                return new ImageCapture[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeString(path);
            parcel.writeInt(adapterPosition);
        }
    }

}
