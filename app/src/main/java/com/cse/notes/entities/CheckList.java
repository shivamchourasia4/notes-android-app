package com.cse.notes.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "checklist", foreignKeys = @ForeignKey(
        entity = Note.class,
        parentColumns = "id",
        childColumns = "ownerNoteId"))
public class CheckList {

    @PrimaryKey(autoGenerate = true)
    private int checklistId;

    @ColumnInfo(name = "is_checked")
    private boolean isChecked = false;

    @ColumnInfo(name = "date_time")
    private String dateTime;

    @ColumnInfo(name = "checklist_content")
    private String checklistContent;

    private String ownerNoteId;

    public CheckList() {
    }

    public CheckList(int checklistId, boolean isChecked, String dateTime, String checklistContent, String ownerNoteId) {
        this.checklistId = checklistId;
        this.isChecked = isChecked;
        this.dateTime = dateTime;
        this.checklistContent = checklistContent;
        this.ownerNoteId = ownerNoteId;
    }

    public String getOwnerNoteId() {
        return ownerNoteId;
    }

    public void setOwnerNoteId(String ownerNoteId) {
        this.ownerNoteId = ownerNoteId;
    }

    public int getChecklistId() {
        return checklistId;
    }

    public void setChecklistId(int checklistId) {
        this.checklistId = checklistId;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getChecklistContent() {
        return checklistContent;
    }

    public void setChecklistContent(String checklistContent) {
        this.checklistContent = checklistContent;
    }

    @Override
    public String toString() {
        return "CheckList{" +
                "checklistId=" + checklistId +
                ", isChecked=" + isChecked +
                ", dateTime='" + dateTime + '\'' +
                ", checklistContent='" + checklistContent + '\'' +
                ", ownerNoteId='" + ownerNoteId + '\'' +
                '}';
    }
}
