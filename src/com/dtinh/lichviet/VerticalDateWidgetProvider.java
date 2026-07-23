package com.dtinh.lichviet;

/** Top-level 1x2 provider for launcher and HyperOS compatibility. */
public final class VerticalDateWidgetProvider extends CalendarWidgetProvider {
    @Override protected int layoutId() { return R.layout.calendar_widget_vertical; }

    @Override protected Class<? extends CalendarWidgetProvider> providerClass() {
        return VerticalDateWidgetProvider.class;
    }
}
