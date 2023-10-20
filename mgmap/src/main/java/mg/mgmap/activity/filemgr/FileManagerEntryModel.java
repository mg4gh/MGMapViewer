package mg.mgmap.activity.filemgr;

import java.io.File;

import mg.mgmap.generic.util.Pref;

public class FileManagerEntryModel {
    private File file;
    private final Pref<Boolean> selected = new Pref<>(Boolean.TRUE);
    public File getFile() {
        return file;
    }
    public void setFile(File file) {
        this.file = file;
    }
    public Pref<Boolean> getSelected() {
        return selected;
    }
    public boolean isSelected() {
        return selected.getValue();
    }

}
