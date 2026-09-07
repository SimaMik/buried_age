package com.sima.buriedage.journal;

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
