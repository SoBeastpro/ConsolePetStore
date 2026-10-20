package petstore.fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.util.Objects;

public final class Fx {
    private Fx() {
    }

    public static String stylesheet() {
        return Objects.requireNonNull(Fx.class.getResource("/petstore/fx/app.css")).toExternalForm();
    }

    public static <T> Loaded<T> load(String path, AppContext context) {
        FXMLLoader loader = new FXMLLoader(Fx.class.getResource(path));
        loader.setControllerFactory(type -> createController(type, context));
        try {
            Parent root = loader.load();
            return new Loaded<>(root, loader.getController());
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось загрузить окно " + path, exception);
        }
    }

    public static Stage dialog(Window owner, String title, Parent root) {
        Stage stage = new Stage();
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.sizeToScene();
        return stage;
    }

    private static Object createController(Class<?> type, AppContext context) {
        try {
            return type.getConstructor(AppContext.class).newInstance(context);
        } catch (NoSuchMethodException exception) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException reflective) {
                throw new IllegalStateException(reflective);
            }
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record Loaded<T>(Parent root, T controller) {
    }
}
