package com.clubsite.api.auth;

public enum Role {
    /** Everything, including user management. */
    SUPER_ADMIN,
    /** Create, edit and delete content; trigger league syncs. */
    EDITOR,
    /** Read-only access to the admin panel (drafts, sync logs). */
    VIEWER
}
