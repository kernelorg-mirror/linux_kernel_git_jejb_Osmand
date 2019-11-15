Implementation Details
======================

The current implementation is based on the nexcloud maps 0.1.2 but
with the extensions storage problem fixed.

When a point is transferred from the phone to the cloud, it has a
<nextcloud-id> tag added to <extensions> from the addFavourite() reply
so we know it is safely on the remote.

The assumption of the initial prototype is that remote deletions
prevail, so points present locally but not remotely are deleted if
they have a <nextcloud-id> tag but are sent to the remote if they
don't (either added offline or added on-line but addition didn't
complete).  Local deletions must be made on-line otherwise the points
will get re-added when the remote makes contact.

Currently all remote and local points are only sync'd once at start of
day, so once that sync is complete only local points are kept in sync;
any remote additions won't be reflected until the app is restarted
(TODO: add a periodic sync?).

Timestamp
---------

We add a local timestamp in an <extensions> <modified> tag on the
local so we can compare modification times to determine if a point
should be updated either locally or remotely based on the newest
timestamp.  We're assuming that the local and remote clocks are
synchronized to the second.

Nextcloud currently has no ability to modify the timestamp, so a local
point is added with the local timestamp but this would be updated to
the remote timestamp when the remote point is added (meaning both
timestamps become the same).  This causes two saves of the gpx file:
the first when the point is created with the local timestamp and the
second when the remote gives us its timestamp and we update the local
file with it.

Rename detection is done via the stored id ... all points in the local
that are on the remote should have one.  For points that don't have an
id (either created while the remote was offline or which got some
error trying to update the remote), we do reconciliation by name.

Periodic Sync
-------------

The plugin is updated to ask for a sync interval and supply it through
a new plugin API getSyncIntervalMillis().

Sync is now kicked off whenever the plugin is set enabled.  However,
there is a problem with enablement on start of day since the
cachedFavorites list is often not initialized, so the
FavouritesDbHelper has a new getInitialized() callback which is only
set to true when the first load of the cachedFavourites list is
finished.

Sync periodicity is achieved by queueing a syncWork job which requeues
itself with a delay of the sync interval after completion provided the
plugin is still active.  When the plugin is set inactive, the
FavouritesDbHelper removes the syncWork job and it gets restarted when
the pluging is set active.  This means you can trigger a resync simply
by enabling and disabling the plugin.

TODOS
=====

Group Operations
----------------

The renaming or recolouring of groups is a bulk operation involcinv
the changing of <category> or <extensions><color> for all the points.
It would be nice to have a nextcloud maps bulk interface for this.

Group Colours
-------------

Currently Nextcloud assigns each group a colour in the web API, but
this colour is generated browser side and isn't stored, so nextcloud
would have to be modified to use <extensions><color> to reflect the
colour groups used by OsmAnd.

Dav like updates and Refresh Token
----------------------------------

Rather than having to poll every point periodically, we'd like some
indication whether the remote dataset has changed, so nextcloud should
provide an API to give the greatest last modified timestamp of every
point.  Any change in this would mean additions.  Note that this still
isn't truly dav like because we still have no handle on deletions.

After this, the local could store the last refresh token (highest
remote modification date) and hand it back to the server to ask for
updates.  The server would then supply only the points which had been
added since that token.  The client can process the additions and
update the token.

Handling deletions in a DAV like way
------------------------------------

Traditionally in DAV, refresh tokens are expected to expire, so we can
give ours a convenient lifetime like a week.  However, it does mean
that at least for that week both the local and the remote need to
remember points that have been deleted.  For the local we can do this
with a <extensions><DELETED> flag and simply remove deleted entries
into a separate array ar load time.  Once the separated array has been
played to the server, entries can be deleted since each client only
has one server.  The server can have more than one client, so would
have to remember deletions for the full week.
