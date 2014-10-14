package org.safehaus.subutai.core.registry.impl;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.safehaus.subutai.core.registry.api.RegistryException;
import org.safehaus.subutai.core.registry.api.Template;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Test for TemplateRegistryImpl
 */
public class TemplateRegistryImplTest
{

    private TemplateRegistryImplExt templateRegistry;
    private TemplateDAO templateDAO;


    static class TemplateRegistryImplExt extends TemplateRegistryImpl
    {
        TemplateRegistryImplExt( final DataSource dataSource ) throws DaoException
        {
            super( dataSource );
        }


        public void setTemplateDao( TemplateDAO templateDao )
        {
            this.templateDAO = templateDao;
        }
    }


    @Before
    public void setUp() throws Exception
    {
        Connection connection = mock( Connection.class );
        DataSource dataSource = mock( DataSource.class );
        PreparedStatement preparedStatement = mock( PreparedStatement.class );
        ResultSet resultSet = mock( ResultSet.class );
        when( connection.prepareStatement( anyString() ) ).thenReturn( preparedStatement );
        when( dataSource.getConnection() ).thenReturn( connection );
        when( preparedStatement.executeQuery() ).thenReturn( resultSet );
        ResultSetMetaData metadata = mock( ResultSetMetaData.class );
        when( metadata.getColumnCount() ).thenReturn( 1 );
        when( metadata.getColumnName( 1 ) ).thenReturn( "info" );
        when( metadata.getColumnType( 1 ) ).thenReturn( java.sql.Types.CLOB );
        when( resultSet.getMetaData() ).thenReturn( metadata );
        when( resultSet.next() ).thenReturn( true ).thenReturn( false );
        templateRegistry = new TemplateRegistryImplExt( dataSource );
        templateDAO = mock( TemplateDAO.class );
        templateRegistry.setTemplateDao( templateDAO );
    }


    @Test( expected = NullPointerException.class )
    public void constructorShouldFailOnNullDataSource() throws Exception
    {
        new TemplateRegistryImpl( null );
    }


    @Test
    public void testRegisterTemplate() throws Exception
    {
        templateRegistry.registerTemplate( TestUtils.CONFIG_FILE, TestUtils.PACKAGES_MANIFEST, TestUtils.MD_5_SUM );

        verify( templateDAO ).saveTemplate( TestUtils.getDefaultTemplate() );
    }


    @Test( expected = RegistryException.class )
    public void testRegisterTemplateException() throws Exception
    {
        Mockito.doThrow( new DaoException( null ) ).when( templateDAO ).saveTemplate( any( Template.class ) );

        templateRegistry.registerTemplate( TestUtils.CONFIG_FILE, TestUtils.PACKAGES_MANIFEST, TestUtils.MD_5_SUM );
    }
}
