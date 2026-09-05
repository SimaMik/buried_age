package com.sima.buriedage.journal;

/** The client's copy of the entry list. A plain holder, so common code can hand it the synced book. */
public final class ClientJournal {
    private static volatile JournalBook book = JournalBook.EMPTY;

    private ClientJournal() {}

    public static JournalBook book() {
        return book;
    }

    public static void accept(JournalBook received) {
        book = received;
    }
}
