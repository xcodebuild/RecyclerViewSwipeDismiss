# RecyclerViewSwipeDismiss
A very easy-to-use and non-intrusive implement of Swipe to dismiss for RecyclerView.

## Preview

![preview](http://i2.tietuku.com/a5a1a6fbd300397a.gif)


## How to use

- Add these lines to your `build.gradle`

```gradle
repositories {
	maven {
	    url "https://jitpack.io"
	}
}

dependencies {
	 compile 'com.github.CodeFalling:RecyclerViewSwipeDismiss:v1.1.0'
}
```

- Build `onTouchListener` and bind it to your `RecyclerView`

```java

SwipeDismissRecyclerViewTouchListener listener = new SwipeDismissRecyclerViewTouchListener.Builder(
        recyclerView,
        new SwipeDismissRecyclerViewTouchListener.DismissCallbacks() {
            @Override
            public boolean canDismiss(int position) {
                return true;
            }

            @Override
            public void onDismiss(View view) {
                // Do what you want when dismiss
                
            }
        })
        .setIsVertical(false)
        .setItemTouchCallback(
                new SwipeDismissRecyclerViewTouchListener.OnItemTouchCallBack() {
                    @Override
                    public void onTouch(int index) {
                    	// Do what you want when item be touched
                    }
                })
        .create();
recyclerView.setOnTouchListener(listener);
```

## More

- `setIsVertical(false)` means allow **swipe in horizontal direction** 

- `listener.setEnabled(false)` can disable swipe to dismiss

- `onTouch` will be called when MOUSE_UP on item without swipe

## Sample

You can see sample code in [`sample/MainActivity.java`](https://github.com/CodeFalling/RecyclerViewSwipeDismiss/blob/master/Sample/app/src/main/java/io/github/codefalling/recyclerviewswipedismiss/sample/MainActivity.java)