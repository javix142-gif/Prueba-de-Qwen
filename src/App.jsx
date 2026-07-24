import './App.css'

function App() {
  return (
    <div style={{ padding: '20px', fontFamily: 'system-ui, sans-serif' }}>
      <h1>Primer Año</h1>
      <p>Aplicación familiar de organización y gastos.</p>
      <div style={{ marginTop: '20px', padding: '15px', backgroundColor: '#f0f0f0', borderRadius: '8px' }}>
        <h2>Estado: Etapa 1 Completada</h2>
        <ul>
          <li>✓ Proyecto React con Vite inicializado</li>
          <li>✓ Supabase client configurado</li>
          <li>✓ Migraciones de base de datos creadas</li>
          <li>✓ Políticas RLS definidas</li>
        </ul>
        <p style={{ color: '#666', fontSize: '14px' }}>
          Próximo paso: Implementar autenticación y asociación de usuarios a familias.
        </p>
      </div>
    </div>
  )
}

export default App
