package com.project.unitube.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.project.unitube.R;
import com.project.unitube.network.RetroFit.RetrofitClient;
import com.project.unitube.utils.manager.UserManager;
import com.project.unitube.entities.Comment;
import com.project.unitube.viewmodel.CommentViewModel;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> comments;
    private CommentAdapterListener listener;

    public interface CommentAdapterListener {
        void onCommentDeleted(int newCommentCount);
    }

    public CommentAdapter(Context context, List<Comment> comments, CommentAdapterListener listener) {
        this.context = context;
        this.comments = comments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        holder.commentUserName.setText(comment.getUserName());
        holder.commentContent.setText(comment.getCommentText());

        // Load user profile image
        String baseUrl = RetrofitClient.getBaseUrl();
        String profilePhotoUrl = baseUrl + comment.getProfilePicture();;  // Combine base URL and path

        Glide.with(context)
                .load(profilePhotoUrl)
                .circleCrop()
                .placeholder(R.drawable.default_profile) // Placeholder in case of loading issues
                .into(holder.commentUserProfileImage);

        // Set up more options button logic
        holder.moreOptionsButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, holder.moreOptionsButton);
            popupMenu.inflate(R.menu.comment_options_menu);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (UserManager.getInstance().getCurrentUser() == null) {
                        // User is not logged in, show notification
                        Toast.makeText(context, "You must be logged in to perform this action.", Toast.LENGTH_SHORT).show();
                        return true;
                    } else {
                        int adapterPosition = holder.getAdapterPosition(); // Use getAdapterPosition()
                        if (adapterPosition == RecyclerView.NO_POSITION) {
                            return false;
                        }
                        if (item.getItemId() == R.id.action_edit) {
                            // Handle edit action
                            showEditDialog(adapterPosition, comment);
                            return true;
                        } else if (item.getItemId() == R.id.action_delete) {
                            // Show confirmation dialog
                            new AlertDialog.Builder(context)
                                    .setTitle("Delete Comment")
                                    .setMessage("Are you sure you want to delete this comment?")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        // Delete the comment from the database
                                        CommentViewModel commentViewModel = new CommentViewModel();
                                        commentViewModel.deleteComment(comment.getId()).observe((LifecycleOwner) context, result -> {
                                            if (result.equals("Success")) {
                                                // Delete the comment
                                                comments.remove(adapterPosition);
                                                notifyItemRemoved(adapterPosition);
                                                notifyItemRangeChanged(adapterPosition, comments.size());
                                                // Notify listener about the deletion
                                                if (listener != null) {
                                                    listener.onCommentDeleted(comments.size());
                                                }
                                                Toast.makeText(context, "Comment deleted successfully.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(context, "Failed to delete comment.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    })
                                    .setNegativeButton("No", null)
                                    .show();
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            });
            popupMenu.show();
        });
    }

    private void showEditDialog(int position, Comment comment) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Comment");

        // Set up the input
        final EditText input = new EditText(context);
        input.setText(comment.getCommentText());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newCommentText = input.getText().toString().trim();
            if (!newCommentText.isEmpty()) {
                comment.setCommentText(newCommentText);

                // Update the comment in the database
                CommentViewModel commentViewModel = new CommentViewModel();
                commentViewModel.updateComment(comment).observe((LifecycleOwner) context, result -> {
                    if (result.equals("Success")) {
                        Toast.makeText(context, "Comment updated successfully.", Toast.LENGTH_SHORT).show();
                        comment.setCommentText(newCommentText);
                        notifyItemChanged(position);
                    } else {
                        Toast.makeText(context, "Failed to update comment.", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(context, "Comment cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView commentUserProfileImage;
        TextView commentUserName;
        TextView commentContent;
        ImageButton moreOptionsButton;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            commentUserProfileImage = itemView.findViewById(R.id.comment_user_profile_image); // ID from round_comment_profile_image.xml
            commentUserName = itemView.findViewById(R.id.comment_user_name);
            commentContent = itemView.findViewById(R.id.comment_content);
            moreOptionsButton = itemView.findViewById(R.id.moreOptionsButton);
        }
    }
}
