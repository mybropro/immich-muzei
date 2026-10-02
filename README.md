![app icon](https://github.com/user-attachments/assets/55bea361-6a4e-44a1-bf95-e8bed641b10a)

# Immich Muzei Plugin

A [Muzei](https://muzei.co/) plugin that displays photos from your [Immich](https://immich.app/) library as your Android wallpaper.

<img height="600" alt="immich-muzei-screenshot" src="https://github.com/user-attachments/assets/467b08e0-431f-474c-8285-04382e6b9da5" />



## Download

You can download the latest release from the [the releases page](https://github.com/abdusco/immich-muzei/releases/latest).


## Current features

- Connect to your Immich server using server URL and API key
- Browse and select albums to display
- Browse and select people (named faces) to display, with a checkbox list and face thumbnails
- Browse and select tags to filter photos
- Filter photos by date range
- Show only favorited photos (optional)
- Exclude the 100 closest semantic matches to a free-text phrase (for example, "bath or tub")
- Automatically rotates through random photos from selected albums/people/tags

### How albums and people combine

Albums and people form a single pool of sources. Each wallpaper refresh draws photos from
one entry in that pool, cycling through them in turn — so selecting two albums and three
people gives you photos from *any* of those five sources, not only photos that satisfy all
of them at once. Tags, "favorites only" and the date filter are applied on top of whichever
source is drawn. Selecting nothing means the whole library.

### Semantic exclusions

In **Exclude photos matching**, enter a phrase and tap **Apply**. On each refresh,
the plugin searches Immich Smart Search with that phrase and the exact same album or
person, tags, favorites and taken-date filters as the random wallpaper request.
It excludes the first 100 returned IDs, then fetches random photos without the text
query and keeps only photos outside that set. Both the Muzei source and the
Immich Random folder provider use this filter.

Smart Search must be enabled on the server and accessible to your API key. The 100
results are ranked matches, not guaranteed classifications: unrelated photos can
be excluded and matching photos beyond the first 100 can remain. If the source has
100 or fewer searchable photos, it may be entirely excluded. Filtering can return
fewer photos or none; the plugin does not refill the batch with excluded photos.
If Smart Search fails, no new photos are supplied. Clearing the text and tapping
**Apply** disables exclusions. Applied filter changes clear queued Muzei photos when
you leave settings; Muzei may keep displaying its current wallpaper until replaced.

- "Open in Immich" action to view the current photo in your Immich instance
- "Add to favorites" action within Muzei UI and using an app shortcut.


## Getting an API key to connect to Immich


1. Log into your Immich web instance.
2. Go to **Account Settings** by clicking on your profile picture in the top right corner.
3. Navigate to the **API Keys** section.
    <img height="400" alt="api keys" src="https://github.com/user-attachments/assets/637e8913-b7b7-441b-8d3f-646a776a9d63" />
4. Click on **New API Key** and give it a name and add all permissions. Your key stays on the device and is never sent to any third party.
    <img height="400" alt="immich_2" src="https://github.com/user-attachments/assets/11e1d89f-c824-42dc-906a-76e5c28f5dbc" />
5. Copy the generated API key and use it in the Immich Muzei plugin to connect to your server.
    <img height="300" alt="immich_3" src="https://github.com/user-attachments/assets/3304fbee-41e6-4761-a598-cb5976071156" />
