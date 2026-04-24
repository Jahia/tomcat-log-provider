import {registry} from '@jahia/ui-extender';
import React from 'react';
import {Text} from '@jahia/moonstone';
import TomcatLogProviderAdmin from './TomcatLogProvider';
import TomcatLogViewer from './TomcatLogViewer';

export default () => {
    console.debug('%c tomcat-log-provider: activation in progress', 'color: #006633');
    registry.add('adminRoute', 'tomcatLog', {
        targets: ['administration-server:99'],
        requiredPermission: 'admin',
        icon: <Text/>,
        label: 'tomcat-log-provider:label.main_menu_entry',
        isSelectable: false
    });
    registry.add('adminRoute', 'tomcatLogProvider', {
        targets: ['administration-server-tomcatLog:1'],
        requiredPermission: 'admin',
        label: 'tomcat-log-provider:label.conf_menu_entry',
        isSelectable: true,
        render: () => React.createElement(TomcatLogProviderAdmin)
    });
    registry.add('adminRoute', 'tomcatLogViewer', {
        targets: ['administration-server-tomcatLog:2'],
        requiredPermission: 'admin',
        label: 'tomcat-log-provider:label.logViewer_menu_entry',
        isSelectable: true,
        render: () => React.createElement(TomcatLogViewer)
    });
};
