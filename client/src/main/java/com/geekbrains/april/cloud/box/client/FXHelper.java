package com.geekbrains.april.cloud.box.client;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class FXHelper {
    public FXHelper() {
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public static class FormattedTableCellFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

        private TextAlignment alignment;

        private FXDecimalFormat format = null;

        public TextAlignment getAlignment() {
            return alignment;
        }

        public void setAlignment(TextAlignment alignment) {
            this.alignment = alignment;
        }

        public FXDecimalFormat getFormat() {
            return format;
        }

        public void setFormat(FXDecimalFormat format) {
            this.format = format;
        }

        @Override
        @SuppressWarnings("unchecked")
        public TableCell<S, T> call(TableColumn<S, T> p) {
            TableCell<S, T> cell = new TableCell<S, T>() {

                @Override
                public void updateItem(Object item, boolean empty) {
                    if (item == getItem()) {
                        return;
                    }
                    super.updateItem((T) item, empty);
                    if (item == null) {
                        super.setText(null);
                        super.setGraphic(null);
                    } else if (format != null) {
                        super.setText(format.format(item));
                    } else if (item instanceof Node) {
                        super.setText(null);
                        super.setGraphic((Node) item);
                    } else {
                        super.setText(item.toString());
                        super.setGraphic(null);
                    }
                }
            };

            switch (alignment) {
                case CENTER:
                    cell.setAlignment(Pos.CENTER);
                    break;
                case RIGHT:
                    cell.setAlignment(Pos.CENTER_RIGHT);
                    break;
                default:
                    cell.setAlignment(Pos.CENTER_LEFT);
                    break;
            }

            return cell;
        }
    }

    public static class FXDecimalFormat extends DecimalFormat {

        public FXDecimalFormat(String pattern, DecimalFormatSymbols symbols) {
            super(pattern, symbols);
        }

        public static FXDecimalFormat valueOf(String pattern) {
            return new FXDecimalFormat(pattern, new DecimalFormatSymbols(Locale.ITALY));
        }
    }


}
