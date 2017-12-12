//! Show paths
import java.io.File
import de.gebit.gsh.TableWriter;

System.out.println();
TableWriter tw = new TableWriter();
tw.column().header("#");
tw.column().header("Path");
def roots = de.gebit.gsh.Gsh.roots;
for (int i = 0; i < roots.size(); i++) {
    File f = roots.get(i);
    tw.row(String.valueOf(i + 1), f.getAbsolutePath());
}
tw.flush();
System.out.println();
