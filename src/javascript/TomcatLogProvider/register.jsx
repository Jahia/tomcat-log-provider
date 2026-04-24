import {registry} from '@jahia/ui-extender';
import {TomcatLogProviderAdmin} from './TomcatLogProvider';
import React from 'react';

export default () => {
    console.debug('%c tomcat-log-provider: activation in progress', 'color: #006633');
    registry.add('adminRoute', 'tomcatLogProvider', {
        targets: ['administration-server-systemComponents:30'],
        requiredPermission: 'admin',
        label: 'tomcat-log-provider:label.menu_entry',
        isSelectable: true,
        render: () => React.createElement(TomcatLogProviderAdmin)
    });
};
