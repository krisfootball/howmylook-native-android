import { createClient } from "npm:@supabase/supabase-js@2.49.1";

type AdminClient = ReturnType<typeof createClient>;

export async function deleteUserAccount(admin: AdminClient, userId: string) {
  const { data: posts, error: postsError } = await admin
    .from("posts")
    .select("id")
    .eq("user_id", userId);

  if (postsError) {
    throw new Error(postsError.message);
  }

  const postIds = (posts ?? []).map((row) => row.id).filter(Boolean);

  if (postIds.length > 0) {
    const { error: postVotesError } = await admin.from("votes").delete().in("post_id", postIds);
    if (postVotesError) {
      throw new Error(postVotesError.message);
    }

    const { error: postImagesError } = await admin.from("post_images").delete().in("post_id", postIds);
    if (postImagesError) {
      throw new Error(postImagesError.message);
    }

    const { error: deletePostsError } = await admin.from("posts").delete().eq("user_id", userId);
    if (deletePostsError) {
      throw new Error(deletePostsError.message);
    }
  }

  const { error: ownVotesError } = await admin.from("votes").delete().eq("user_id", userId);
  if (ownVotesError) {
    throw new Error(ownVotesError.message);
  }

  const { error: followerRowsError } = await admin.from("follows").delete().eq("follower_id", userId);
  if (followerRowsError) {
    throw new Error(followerRowsError.message);
  }

  const { error: followingRowsError } = await admin.from("follows").delete().eq("following_id", userId);
  if (followingRowsError) {
    throw new Error(followingRowsError.message);
  }

  const { error: pushDevicesError } = await admin.from("android_push_devices").delete().eq("user_id", userId);
  if (pushDevicesError) {
    throw new Error(pushDevicesError.message);
  }

  const { error: notificationsError } = await admin.from("user_notifications").delete().eq("user_id", userId);
  if (notificationsError) {
    throw new Error(notificationsError.message);
  }

  await admin.from("account_deletion_requests").delete().eq("user_id", userId);

  await removeStoragePrefix(admin, "post-images", userId);
  await removeStoragePrefix(admin, "profile-avatars", userId);

  const { error: profileError } = await admin.from("profiles").delete().eq("id", userId);
  if (profileError) {
    throw new Error(profileError.message);
  }

  const { error: deleteAuthError } = await admin.auth.admin.deleteUser(userId);
  if (deleteAuthError) {
    throw new Error(deleteAuthError.message);
  }
}

async function removeStoragePrefix(admin: AdminClient, bucket: string, prefix: string) {
  const { data, error } = await admin.storage.from(bucket).list(prefix, { limit: 1000 });
  if (error || !data?.length) {
    return;
  }

  const paths = data
    .map((item) => item.name)
    .filter(Boolean)
    .map((name) => `${prefix}/${name}`);

  if (paths.length > 0) {
    await admin.storage.from(bucket).remove(paths);
  }
}
