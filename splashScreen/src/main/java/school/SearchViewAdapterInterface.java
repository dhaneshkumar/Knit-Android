package school;

import android.widget.BaseAdapter;
import android.widget.Filterable;

/**
 * Created by ashish on 10/9/15.
 */

abstract class SearchViewAdapterInterface extends BaseAdapter implements AdapterItemDescription, Filterable {
}

interface AdapterItemDescription{
    String getStringDescription(int position);
}