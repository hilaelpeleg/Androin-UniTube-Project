package com.project.unitube.ui.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
//import com.project.unitube.Room.Dao.UserDao;
import com.project.unitube.Room.Dao.CommentDao;
import com.project.unitube.Room.Database.AppDB;
import com.project.unitube.network.RetroFit.RetrofitClient;
import com.project.unitube.utils.helper.DarkModeHelper;
import com.project.unitube.utils.helper.NavigationHelper;
import com.project.unitube.R;
import com.project.unitube.utils.manager.UserManager;
import com.project.unitube.ui.adapter.VideoAdapter;
import com.project.unitube.entities.User;
import com.project.unitube.entities.Video;
import com.project.unitube.viewmodel.CommentViewModel;
import com.project.unitube.viewmodel.UserViewModel;
import com.project.unitube.viewmodel.VideoViewModel;

import static com.project.unitube.utils.VideoInteractionHandler.updateDate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private static final int ADD_VIDEO_REQUEST = 1; // Request code for adding a video
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final int CAPTURE_IMAGE_REQUEST = 3;

    private Uri editDialogSelectedPhotoUri;
    private ImageView editDialogprofileImageView;

    private DrawerLayout drawerLayout;
    private NavigationHelper navigationHelper;
    private DarkModeHelper darkModeHelper;
    private VideoAdapter videoAdapter;

    private UserViewModel userViewModel;
    private VideoViewModel videoViewModel;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the UI components. Binds the XML views to the corresponding Java objects.
        initializeUIComponents();

        // Initialize the ViewModels
        initializeViewModels();

        // Set up listeners for the buttons in the activity.
        setUpListeners();

        // Initialize VideosToShow with all videos
        initializeVideosToShow();

    }


    private void initializeViewModels() {
        userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
    }

    private void initializeUIComponents() {
        if (updateDate) {
            updateDate = false;
            onResume();
        }
        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Initialize RecyclerView
        RecyclerView videoRecyclerView = findViewById(R.id.videoRecyclerView);
        videoRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Set the adapter for the RecyclerView using the global videos list
        videoAdapter = new VideoAdapter(this);
        videoRecyclerView.setAdapter(videoAdapter);

        // Initialize NavigationHelper
        navigationHelper = new NavigationHelper(this, drawerLayout, videoRecyclerView);
        navigationHelper.initializeNavigation(navigationView);

        // Initialize DarkModeHelper
        darkModeHelper = new DarkModeHelper(this);

        // Initialize dark mode buttons
        darkModeHelper.initializeDarkModeButtons(
                findViewById(R.id.button_toggle_mode)
        );

        // Initialize the auth button (Sign In/Sign Out)
        initLoginSignOutButton();

        // Initialize search functionality
        initializeSearchFunctionality();
    }

    private void initLoginSignOutButton() {
        // Find the LinearLayout and its components
        LinearLayout authLinearLayout = findViewById(R.id.log_in_out_button_layout);
        TextView authText = findViewById(R.id.text_log_in_out);
        ImageView authIcon = findViewById(R.id.icon_log_in_out);
        UserManager userManager = UserManager.getInstance();

        // Check if there is a logged-in user
        if (userManager.getCurrentUser() == null) {
            // No user logged in, set to "Sign In"
            authText.setText(getString(R.string.sign_in));
            authIcon.setImageResource(R.drawable.ic_login); // Change icon if needed

            authLinearLayout.setOnClickListener(view -> {
                Intent intent = new Intent(MainActivity.this, LoginScreen.class);
                startActivity(intent);
            });
        } else {
            // User is logged in, set to "Sign Out"
            authText.setText(getString(R.string.sign_out));
            authIcon.setImageResource(R.drawable.ic_logout); // Change icon if needed

            authLinearLayout.setOnClickListener(view -> {
                // Handle sign out and go to LoginScreen
                Toast.makeText(this, "User signed out", Toast.LENGTH_SHORT).show();
                userManager.setCurrentUser(null);

                // Navigate to LoginScreen after sign out
                Intent intent = new Intent(MainActivity.this, LoginScreen.class);
                startActivity(intent);
            });

            // Update user greeting and profile picture
            updateGreetingUser();
        }
    }

    private void InitDeleteAndEditAccountButtons() {
        LinearLayout deleteAccountButton = findViewById(R.id.delete_user_button_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        ImageView editUserButton = headerView.findViewById(R.id.edit_user_button); // Access from headerView

        User currentUser = UserManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            // Make buttons visible
            deleteAccountButton.setVisibility(View.VISIBLE);
            editUserButton.setVisibility(View.VISIBLE);

            // Set up listener for delete account button
            deleteAccountButton.setOnClickListener(view -> {

                // Delete the user account
                userViewModel.deleteUser(currentUser.getUserName()).observe(this, result -> {
                    // Handle the result with observer to the response
                    if (result.equals("success")) {
                        // Notify the user, set current user to null and navigate to LoginScreen
                        UserManager.getInstance().setCurrentUser(null);
                        Toast.makeText(this, "User account deleted", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(MainActivity.this, LoginScreen.class);
                        startActivity(intent);
                    } else if (result.equals("403") || result.equals("401")) {
                        Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to delete user", Toast.LENGTH_SHORT).show();
                    }
                });
            });


            // Set up listener for edit user button
            editUserButton.setOnClickListener(view -> showEditUserDialog());
        } else {
            // Hide buttons if no user is logged in
            deleteAccountButton.setVisibility(View.GONE);
            editUserButton.setVisibility(View.GONE);
        }
    }

    private void showEditUserDialog() {
        // Create an AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Inflate the custom dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_user, null);
        builder.setView(dialogView);

        // Reference the EditText fields
        EditText firstNameEditText = dialogView.findViewById(R.id.edit_first_name);
        EditText lastNameEditText = dialogView.findViewById(R.id.edit_last_name);
        EditText passwordEditText = dialogView.findViewById(R.id.edit_password);
        EditText reEnterPasswordEditText = dialogView.findViewById(R.id.edit_reEnter_password);
        editDialogprofileImageView = dialogView.findViewById(R.id.edit_profile_picture);
        Button changeProfilePictureButton = dialogView.findViewById(R.id.change_profile_picture_button);

        // Get the current user and set the EditText fields
        User user = UserManager.getInstance().getCurrentUser();
        firstNameEditText.setText(user.getFirstName());
        lastNameEditText.setText(user.getLastName());
        passwordEditText.setText(user.getPassword());
        reEnterPasswordEditText.setText(user.getPassword());

        // Load current profile picture (if any)
        if (user.getProfilePicture() != null) {
            // Construct the full profile picture URL
            String baseUrl = RetrofitClient.getBaseUrl();
            String profilePhotoUrl = baseUrl + user.getProfilePicture();  // Combine base URL and path

            Glide.with(this)
                    .load(profilePhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.default_profile) // Placeholder in case of loading issues
                    .into(editDialogprofileImageView);
        }

        // Set up the profile picture change listener
        changeProfilePictureButton.setOnClickListener(view -> {
            // Call UploadPhotoHelper to choose or take a new profile picture
            showImagePickerOptions();
        });



        // Set up the "Save" button
        builder.setPositiveButton("Save", null); // Null listener to prevent dialog from closing in case of invalid input

        // Set up the "Cancel" button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss(); // Simply dismiss the dialog
            }
        });

        // Create and show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle the "Save" button click
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            // Get updated values from EditTexts
            String updatedFirstName = firstNameEditText.getText().toString();
            String updatedLastName = lastNameEditText.getText().toString();
            String updatedPassword = passwordEditText.getText().toString();

            // Validate the input and update the user data
            if (validateFields(firstNameEditText, lastNameEditText, passwordEditText, reEnterPasswordEditText)) {
                // Update the user object
                user.setFirstName(updatedFirstName);
                user.setLastName(updatedLastName);
                user.setPassword(updatedPassword);

                // Update the user profile picture (if changed)
                if (getEditDialogSelectedPhotoUri() != null) {
                    String updatedProfilePictureUri = getEditDialogSelectedPhotoUri().toString();
                    user.setProfilePicture(updatedProfilePictureUri);
                }
                String updatedProfilePictureUri = getEditDialogSelectedPhotoUri().toString();

                userViewModel.updateUser(user, editDialogSelectedPhotoUri).observe(this, result -> {
                    // Handle the result with observer to the response
                    if (result.equals("success")) {
                        Toast.makeText(this, "User updated successfully", Toast.LENGTH_SHORT).show();
                        updateGreetingUser();
                        updateProfilePhotoPresent();
                        dialog.dismiss(); // Dismiss the dialog only if the update is successful
                    } else if (result.equals("invalid token")) {
                        Toast.makeText(this, "Unauthorized access", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Failed to update user", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private boolean validateFields(EditText firstNameEditText, EditText lastNameEditText, EditText passwordEditText, EditText reEnterPasswordEditText) {
        boolean isValid = true;

        // Check if first name is empty or contains numbers
        String firstName = firstNameEditText.getText().toString();
        if (TextUtils.isEmpty(firstName)) {
            firstNameEditText.setError("First name is required");
            isValid = false;
        } else if (firstName.matches(".*\\d.*")) {
            firstNameEditText.setError("First name should contain letters only");
            isValid = false;
        }

        // Check if last name is empty or contains numbers
        String lastName = lastNameEditText.getText().toString();
        if (TextUtils.isEmpty(lastName)) {
            lastNameEditText.setError("Last name is required");
            isValid = false;
        } else if (lastName.matches(".*\\d.*")) {
            lastNameEditText.setError("Last name should contain letters only");
            isValid = false;
        }

        // Check if password is empty, contains non-alphanumeric characters, or less than 8 characters.
        String password = passwordEditText.getText().toString();
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            isValid = false;
        } else if (!password.matches("[a-zA-Z0-9]+")) {
            passwordEditText.setError("Password should contain only letters and numbers");
        } else if (password.length() < 8) {
            passwordEditText.setError("Password must be at least 8 characters");
            isValid = false;
        }

        // Check if re-enter password is empty or do not match password
        String reEnterPassword = reEnterPasswordEditText.getText().toString();
        if (TextUtils.isEmpty(reEnterPassword)) {
            reEnterPasswordEditText.setError("Re-entering password is required");
            isValid = false;
        } else if (!reEnterPassword.equals(password)) {
            reEnterPasswordEditText.setError("Passwords do not match");
            isValid = false;
        }

        return isValid;
    }

    private void setUpListeners() {
        // Set up action_menu button to open the drawer
        findViewById(R.id.action_menu).setOnClickListener(view -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // Initialize Bottom Navigation
        findViewById(R.id.button_add_video).setOnClickListener(view -> {
            if (UserManager.getInstance().getCurrentUser() != null) {
                // Navigate to add video screen
                Intent intent = new Intent(MainActivity.this, AddVideoScreen.class);
                startActivityForResult(intent, ADD_VIDEO_REQUEST); // Start AddVideoScreen with request code
            } else {
                Toast.makeText(this, "Log in first", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(MainActivity.this, LoginScreen.class);
                startActivity(intent);
            }
        });
    }

    private void initializeVideosToShow() {
        videoViewModel.getVideos().observe(this, videos -> {
            videoAdapter.setVideos(videos);
        });
        videoViewModel.getVideos();
    }

    private void initializeSearchFunctionality() {
        EditText searchBox = findViewById(R.id.search_box);
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterVideos(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });
    }

    private void filterVideos(String query) {
        videoViewModel.getVideos().observe(this, videos -> {
            List<Video> filteredVideos;
            if (query.isEmpty()) {
                filteredVideos = videos;
            } else {
                String lowerCaseQuery = query.toLowerCase();
                filteredVideos = videos.stream()
                        .filter(video ->
                                video.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                                        video.getDescription().toLowerCase().contains(lowerCaseQuery) ||
                                        video.getUploader().toLowerCase().contains(lowerCaseQuery))
                        .collect(Collectors.toList());
            }
            videoAdapter.setVideos(filteredVideos);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateGreetingUser();
        updateProfilePhotoPresent();
        initLoginSignOutButton();
        initializeVideosToShow(); // Ensure the videos list is updated
        videoAdapter.notifyDataSetChanged(); // Refresh the adapter
        InitDeleteAndEditAccountButtons();
    }

    private void updateProfilePhotoPresent() {
        ImageView currentUserProfilePic = findViewById(R.id.logo);
        User currentUser = UserManager.getInstance().getCurrentUser();

        if (currentUser != null) {

            if (currentUser.getProfilePicture() != null) {
                // Construct the full profile picture URL
                String baseUrl = RetrofitClient.getBaseUrl();
                Log.d("ProfilePhoto", "Base URL: " + baseUrl);

                String profilePhotoUrl = baseUrl + currentUser.getProfilePicture();  // Combine base URL and path
                Log.d("ProfilePhoto", "Profile Photo: " +currentUser.getProfilePicture());
                Log.d("ProfilePhoto", "Profile photo URL: " + profilePhotoUrl);


                Glide.with(this)
                        .load(profilePhotoUrl)
                        .circleCrop()
                        .placeholder(R.drawable.default_profile) // Placeholder in case of loading issues
                        .into(currentUserProfilePic);
            } else {
                Uri profilePhotoUri = Uri.parse(currentUser.getProfilePicture());

                currentUserProfilePic.setImageResource(R.drawable.default_profile);
                Glide.with(this)
                        .load(profilePhotoUri)  // This line seems redundant as profilePhotoUri is null here.
                        .circleCrop()
                        .placeholder(R.drawable.default_profile) // Placeholder in case of loading issues
                        .into(currentUserProfilePic);
            }

            currentUserProfilePic.setOnClickListener(view -> {
                Intent intent = new Intent(MainActivity.this, UserPageActivity.class);
                intent.putExtra("USER", currentUser);
                startActivity(intent);
            });
        } else {
            currentUserProfilePic.setImageResource(R.drawable.unitube_logo);
            currentUserProfilePic.setBackground(null); // Remove background for default logo
            currentUserProfilePic.setScaleType(ImageView.ScaleType.FIT_XY); // Reverting scale type for logo
        }
    }

    private void updateGreetingUser() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        View headerView = navigationView.getHeaderView(0);
        TextView greetingText = headerView.findViewById(R.id.user_greeting);

        User currentUser = UserManager.getInstance().getCurrentUser();

        if (currentUser != null) {
            // User is signed in
            String welcome = getString(R.string.welcome);
            greetingText.setText(welcome + " " + currentUser.getFirstName());

        } else {
            if (greetingText != null) {
                greetingText.setText(R.string.welcome_to_unitube);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            handleActivityResult(requestCode, resultCode, data);
        }
    }

    public void handleActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ADD_VIDEO_REQUEST && resultCode == RESULT_OK) {
            // Refresh the video list when a new video is added
            initializeVideosToShow();
            videoAdapter.notifyDataSetChanged();
        }
        if (requestCode == CAPTURE_IMAGE_REQUEST && resultCode == RESULT_OK) {
            editDialogprofileImageView.setImageURI(editDialogSelectedPhotoUri);
            saveImageFromUri(editDialogSelectedPhotoUri);
        }
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            editDialogSelectedPhotoUri = data.getData(); // Get the URI from the result
            editDialogprofileImageView.setImageURI(editDialogSelectedPhotoUri);
            editDialogprofileImageView.setTag(editDialogSelectedPhotoUri.toString());
            saveImageFromUri(editDialogSelectedPhotoUri); // Call the method to save the image
        }
    }

    public void showImagePickerOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(new CharSequence[]{"Choose from Gallery", "Take a Photo"},
                (dialog, which) -> {
                    switch (which) {
                        case 0:
                            pickImageFromGallery();
                            break;
                        case 1:
                            captureImageFromCamera();
                            break;
                    }
                });
        builder.show();
    }

    private void pickImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    private void captureImageFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                editDialogSelectedPhotoUri = FileProvider.getUriForFile(this, "com.project.unitube.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, editDialogSelectedPhotoUri);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE_REQUEST);
            } else {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera application found", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
        }
        return image;
    }

    private void saveImageFromUri(Uri uri) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_profile.jpg";  // Unique filename
        File imageFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), imageFileName);

        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            this.editDialogSelectedPhotoUri = Uri.parse(imageFile.getAbsolutePath());
            Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    public Uri getEditDialogSelectedPhotoUri() {
        return editDialogSelectedPhotoUri;
    }
}